package com.goodspace.runny.domain.crew.dto;

import com.goodspace.runny.domain.crew.entity.Crew;
import com.goodspace.runny.domain.crew.entity.CrewRole;
import com.goodspace.runny.domain.user.dto.UserSummary;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 크루 요청/응답 DTO 모음. 유저 정보는 6단계 UserSummary 공통 DTO를 재사용한다.
 */
public final class CrewDto {

    private CrewDto() {
    }

    /** 크루명 중복확인 응답 */
    public record NameCheckResponse(
            boolean available
    ) {
    }

    /** 검색 항목 - memberCount와 myRequestStatus(NONE/PENDING)로 가입 신청/승인대기 버튼 분기, 누적 거리 표시 포함 */
    public record SearchItem(
            Long crewId,
            String name,
            String imageUrl,
            String intro,
            int memberCount,
            int maxMembers,
            double totalDistance,
            String myRequestStatus
    ) {
    }

    /** 검색 응답 - 크루명 부분 일치, 전체 반환 (MVP 규모에서 페이징 제거) */
    public record SearchResponse(
            List<SearchItem> content
    ) {
    }

    /** 이번 주 거리 랭킹 항목 - rank는 1~3, distanceKm는 이번 주 누적 거리 */
    public record TopMember(
            int rank,
            UserSummary user,
            double distanceKm
    ) {
    }

    /** 크루원 목록 항목 - 이번 주 러닝 지표 포함(기록이 없으면 null로 내려가 프론트가 "-" 처리) */
    public record MemberItem(
            UserSummary user,
            CrewRole role,
            Double weeklyDistanceKm,
            Long weeklyDurationSec,
            Long weeklyAvgPaceSec
    ) {
    }

    /**
     * 크루 상세 - 미가입자 검색 팝업과 크루원 메인 화면이 동일 데이터 사용.
     * 헤더는 누적(크루원 수/누적 러닝 횟수/누적 거리)과 이번 주 거리(weeklyDistanceKm)를 함께 제공하고,
     * weeklyTopRanking은 이번 주(월 00:00 ~ 일 24:00 KST) 거리 합계 상위 3명이다.
     */
    public record DetailResponse(
            Long crewId,
            String name,
            String imageUrl,
            String intro,
            int memberCount,
            int maxMembers,
            int totalRunCount,
            double totalDistance,
            int weeklyGoalKm,
            double weeklyDistanceKm,
            int weeklyGoalPercent,
            List<TopMember> weeklyTopRanking,
            List<MemberItem> members
    ) {
    }

    /** 내 크루 응답 - 크루장이면 pendingRequestCount(관리 버튼 빨간 배지) 포함 */
    public record MyCrewResponse(
            boolean joined,
            Long crewId,
            String name,
            String imageUrl,
            CrewRole role,
            Integer pendingRequestCount
    ) {
        public static MyCrewResponse none() {
            return new MyCrewResponse(false, null, null, null, null, null);
        }

        public static MyCrewResponse of(Crew crew, CrewRole role, Integer pendingRequestCount) {
            return new MyCrewResponse(true, crew.getId(), crew.getName(),
                    crew.displayImageUrl(), role, pendingRequestCount);
        }
    }

    /** 가입 신청 목록 항목 - 신청자는 UserSummary로 강아지 외형 포함 */
    public record JoinRequestItem(
            Long requestId,
            UserSummary user
    ) {
    }

    /** 일괄 승인/거절 요청 - 요청 ID 배열 */
    public record BatchRequest(
            @NotEmpty List<Long> requestIds
    ) {
    }

    /** 일괄 처리 결과 - 정원 초과 등으로 실패한 건은 사유와 함께 반환 */
    public record BatchResult(
            List<Long> succeededIds,
            List<FailedItem> failed
    ) {
        public record FailedItem(Long requestId, String reason) {
        }
    }
}
