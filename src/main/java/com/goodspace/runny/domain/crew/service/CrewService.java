package com.goodspace.runny.domain.crew.service;

import com.goodspace.runny.domain.crew.dto.CrewDto;
import com.goodspace.runny.domain.crew.entity.Crew;
import com.goodspace.runny.domain.crew.entity.CrewJoinRequest;
import com.goodspace.runny.domain.crew.entity.CrewMember;
import com.goodspace.runny.domain.crew.entity.CrewRole;
import com.goodspace.runny.domain.crew.entity.JoinRequestStatus;
import com.goodspace.runny.domain.crew.repository.CrewJoinRequestRepository;
import com.goodspace.runny.domain.crew.repository.CrewMemberRepository;
import com.goodspace.runny.domain.crew.repository.CrewRepository;
import com.goodspace.runny.domain.user.dto.UserSummary;
import com.goodspace.runny.domain.user.service.UserSummaryService;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import com.goodspace.runny.global.util.S3Uploader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 크루 서비스. 크루명 중복확인, 생성, 검색, 상세, 내 크루 조회, 가입 신청/취소, 탈퇴를 담당한다.
 * 크루장 전용 관리 기능은 CrewAdminService가 담당한다.
 */
@Service
@RequiredArgsConstructor
public class CrewService {

    // S3 크루 로고 프리픽스 (문서 8.4)
    static final String IMAGE_PREFIX = "crew/";

    private final CrewRepository crewRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final CrewJoinRequestRepository crewJoinRequestRepository;
    private final CrewValidator crewValidator;
    private final CrewRunningStatsProvider crewRunningStatsProvider;
    private final UserSummaryService userSummaryService;
    private final S3Uploader s3Uploader;
    private final CrewNotificationHook crewNotificationHook;

    /** 크루명 사용 가능 여부 - 형식/비속어 위반은 예외, 중복이면 false */
    @Transactional(readOnly = true)
    public boolean isNameAvailable(String name) {
        crewValidator.validateNameFormat(name);
        return !crewRepository.existsByName(name);
    }

    /** 크루 생성 - 로고는 선택(있으면 S3 crew/ 업로드, 없으면 기본 이미지), 생성자는 LEADER */
    @Transactional
    public Long create(Long userId, String name, String intro, MultipartFile image) {
        if (crewMemberRepository.existsByUserId(userId)) {
            throw new BusinessException(ErrorCode.CREW_005);
        }
        crewValidator.validateName(name);
        crewValidator.validateIntro(intro);

        // S3 업로드는 트랜잭션 보호를 받지 못하므로, 업로드 후 DB 저장 실패 시 새 객체를 보상 삭제한다 (고아 객체 방지)
        String imageUrl = (image == null || image.isEmpty()) ? null : s3Uploader.upload(image, IMAGE_PREFIX);
        try {
            Crew crew = crewRepository.save(new Crew(name, imageUrl, intro, userId));
            crewMemberRepository.save(new CrewMember(crew.getId(), userId, CrewRole.LEADER));
            // 크루 소속이 되었으므로 다른 크루에 보낸 대기 중 신청은 정리
            crewJoinRequestRepository.deleteAllByUserId(userId);
            // 크루 생성자도 크루 가입으로 보고 "친구들과 달리기" 업적 판정
            crewNotificationHook.onCrewCreated(userId);
            return crew.getId();
        } catch (RuntimeException e) {
            if (imageUrl != null) {
                s3Uploader.delete(imageUrl);
            }
            throw e;
        }
    }

    /** 크루 검색 - 부분 일치, 전체 반환(MVP 규모 페이징 제거). memberCount와 myRequestStatus(NONE/PENDING) 포함 */
    @Transactional(readOnly = true)
    public CrewDto.SearchResponse search(Long userId, String name) {
        List<Crew> crews = crewRepository.findByNameContainingOrderByIdAsc(name);
        List<Long> crewIds = crews.stream().map(Crew::getId).toList();

        // 현재 인원 일괄 집계 + 내 대기 중 신청 크루 목록
        Map<Long, Integer> memberCounts = new HashMap<>();
        if (!crewIds.isEmpty()) {
            crewMemberRepository.countByCrewIds(crewIds).forEach(row ->
                    memberCounts.put((Long) row[0], ((Long) row[1]).intValue()));
        }
        List<Long> myPendingCrewIds = crewJoinRequestRepository.findPendingCrewIdsOf(userId);

        List<CrewDto.SearchItem> content = crews.stream()
                .map(crew -> new CrewDto.SearchItem(
                        crew.getId(), crew.getName(), crew.displayImageUrl(), crew.getIntro(),
                        memberCounts.getOrDefault(crew.getId(), 0), crew.getMaxMembers(),
                        Math.round(crew.getTotalDistance() * 10) / 10.0,
                        myPendingCrewIds.contains(crew.getId()) ? "PENDING" : "NONE"))
                .toList();
        return new CrewDto.SearchResponse(content);
    }

    /**
     * 크루 상세 - 헤더 통계(크루원 수/누적 러닝 횟수/누적 거리)는 누적 기준,
     * 진행 바(주간 목표 대비)와 카테고리별 TOP, 크루원 지표는 이번 주(월요일 00:00 KST) 기준이다.
     */
    @Transactional(readOnly = true)
    public CrewDto.DetailResponse getDetail(Long crewId) {
        Crew crew = findCrew(crewId);
        List<CrewMember> members = crewMemberRepository.findByCrewIdOrderByJoinedAtAsc(crewId);

        // 멤버 요약 일괄 조립 (UserSummaryService 재사용)
        List<Long> memberUserIds = members.stream().map(CrewMember::getUserId).toList();
        Map<Long, UserSummary> summaries = userSummaryService.summarizeAll(memberUserIds);

        // 이번 주 크루원별 러닝 집계 - 기록이 없는 멤버는 맵에 없다
        Map<Long, CrewRunningStatsProvider.WeeklyStats> weeklyStats = crewRunningStatsProvider.weeklyStats(crewId)
                .stream()
                .collect(Collectors.toMap(CrewRunningStatsProvider.WeeklyStats::userId, stats -> stats));

        List<CrewDto.MemberItem> memberItems = members.stream()
                .filter(member -> summaries.containsKey(member.getUserId()))
                .map(member -> {
                    CrewRunningStatsProvider.WeeklyStats stats = weeklyStats.get(member.getUserId());
                    return new CrewDto.MemberItem(
                            summaries.get(member.getUserId()), member.getRole(),
                            stats == null ? null : round1(stats.distanceKm()),
                            stats == null ? null : stats.durationSec(),
                            stats == null ? null : stats.avgPaceSec());
                })
                .toList();

        // 주간 목표 진행 바 - 크루원 전체의 이번 주 거리 합산
        double weeklyDistanceKm = weeklyStats.values().stream()
                .mapToDouble(CrewRunningStatsProvider.WeeklyStats::distanceKm)
                .sum();
        int weeklyGoalPercent = crew.getWeeklyGoalKm() > 0
                ? (int) Math.round(weeklyDistanceKm * 100 / crew.getWeeklyGoalKm())
                : 0;

        return new CrewDto.DetailResponse(crew.getId(), crew.getName(), crew.displayImageUrl(),
                crew.getIntro(), members.size(), crew.getMaxMembers(),
                crew.getTotalRunCount(), round1(crew.getTotalDistance()),
                crew.getWeeklyGoalKm(), round1(weeklyDistanceKm), weeklyGoalPercent,
                buildWeeklyTop(weeklyStats.values(), summaries), memberItems);
    }

    /**
     * 이번 주 카테고리별 TOP 조립 - 스피드(최단 평균 페이스 1회) / 거리(주간 누적) / 체력(주간 누적 시간).
     * 이번 주 기록이 없으면 각 항목은 null로 내려간다(프론트 빈 값 표시).
     */
    private CrewDto.WeeklyTop buildWeeklyTop(Collection<CrewRunningStatsProvider.WeeklyStats> stats,
                                             Map<Long, UserSummary> summaries) {
        List<CrewRunningStatsProvider.WeeklyStats> candidates = stats.stream()
                .filter(entry -> summaries.containsKey(entry.userId()))
                .toList();
        CrewDto.TopMember speed = candidates.stream()
                .filter(entry -> entry.bestPaceSec() > 0)
                .min(Comparator.comparingLong(CrewRunningStatsProvider.WeeklyStats::bestPaceSec))
                .map(entry -> new CrewDto.TopMember(summaries.get(entry.userId()), entry.bestPaceSec()))
                .orElse(null);
        CrewDto.TopMember distance = candidates.stream()
                .max(Comparator.comparingDouble(CrewRunningStatsProvider.WeeklyStats::distanceKm))
                .map(entry -> new CrewDto.TopMember(summaries.get(entry.userId()), round1(entry.distanceKm())))
                .orElse(null);
        CrewDto.TopMember stamina = candidates.stream()
                .max(Comparator.comparingLong(CrewRunningStatsProvider.WeeklyStats::durationSec))
                .map(entry -> new CrewDto.TopMember(summaries.get(entry.userId()), entry.durationSec()))
                .orElse(null);
        return new CrewDto.WeeklyTop(speed, distance, stamina);
    }

    /** 거리 표시용 소수 첫째 자리 반올림 */
    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    /** 내 크루 조회 - role 포함, 크루장이면 pendingRequestCount(대기 중 가입 신청 수) 포함 */
    @Transactional(readOnly = true)
    public CrewDto.MyCrewResponse getMyCrew(Long userId) {
        return crewMemberRepository.findByUserId(userId)
                .map(member -> {
                    Crew crew = findCrew(member.getCrewId());
                    Integer pendingCount = member.getRole() == CrewRole.LEADER
                            ? crewJoinRequestRepository.countByCrewIdAndStatus(crew.getId(), JoinRequestStatus.PENDING)
                            : null;
                    return CrewDto.MyCrewResponse.of(crew, member.getRole(), pendingCount);
                })
                .orElse(CrewDto.MyCrewResponse.none());
    }

    /** 가입 신청 - 1인 1크루, 중복 신청 불가, 정원 초과 크루는 신청 불가. 정원 검증은 크루 행 락으로 직렬화 */
    @Transactional
    public void requestJoin(Long userId, Long crewId) {
        if (crewMemberRepository.existsByUserId(userId)) {
            throw new BusinessException(ErrorCode.CREW_005);
        }
        Crew crew = findCrewForUpdate(crewId);
        if (crewJoinRequestRepository.existsByCrewIdAndUserIdAndStatus(crewId, userId, JoinRequestStatus.PENDING)) {
            throw new BusinessException(ErrorCode.CREW_007);
        }
        if (crewMemberRepository.countByCrewId(crewId) >= crew.getMaxMembers()) {
            throw new BusinessException(ErrorCode.CREW_008);
        }
        crewJoinRequestRepository.save(new CrewJoinRequest(crewId, userId));
    }

    /** 가입 신청 취소 - 본인 PENDING 신청만 */
    @Transactional
    public void cancelJoin(Long userId, Long crewId) {
        CrewJoinRequest request = crewJoinRequestRepository
                .findByCrewIdAndUserIdAndStatus(crewId, userId, JoinRequestStatus.PENDING)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_012));
        crewJoinRequestRepository.delete(request);
    }

    /** 크루 탈퇴 - 일반 크루원만 가능, 크루장은 위임 또는 해체 후 탈퇴 */
    @Transactional
    public void leave(Long userId, Long crewId) {
        CrewMember member = crewMemberRepository.findByCrewIdAndUserId(crewId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_011));
        if (member.getRole() == CrewRole.LEADER) {
            throw new BusinessException(ErrorCode.CREW_010);
        }
        crewMemberRepository.delete(member);
    }

    /**
     * 회원 탈퇴 훅 (UserService에서 호출) - 크루장이고 크루원이 있으면 위임 필요 에러,
     * 크루장 혼자면 크루 해체(S3 로고 삭제 포함), 일반 크루원이면 멤버십 삭제. 본인 가입 신청도 전부 정리한다.
     */
    @Transactional
    public void handleUserWithdrawal(Long userId) {
        crewMemberRepository.findByUserId(userId).ifPresent(member -> {
            if (member.getRole() == CrewRole.LEADER) {
                int memberCount = crewMemberRepository.countByCrewId(member.getCrewId());
                if (memberCount > 1) {
                    throw new BusinessException(ErrorCode.CREW_010);
                }
                // 크루장 혼자인 크루는 해체 후 탈퇴 (임의 설정, 문서 4.A)
                Crew crew = findCrew(member.getCrewId());
                String imageUrl = crew.getImageUrl();
                crewJoinRequestRepository.deleteAllByCrewId(member.getCrewId());
                crewMemberRepository.delete(member);
                crewRepository.deleteById(member.getCrewId());
                // S3 로고 삭제 - 커밋 후 수행 (문서 8.4 삭제 정책)
                if (imageUrl != null) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            s3Uploader.delete(imageUrl);
                        }
                    });
                }
            } else {
                crewMemberRepository.delete(member);
            }
        });
        crewJoinRequestRepository.deleteAllByUserId(userId);
    }

    /** 크루 조회 (비관적 쓰기 락) - 정원 검증/변경 흐름에서 사용. 동시 승인/신청 간 정원 초과 방지 */
    Crew findCrewForUpdate(Long crewId) {
        return crewRepository.findByIdForUpdate(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_006));
    }

    /** 크루 조회 공통 */
    Crew findCrew(Long crewId) {
        return crewRepository.findById(crewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREW_006));
    }
}
