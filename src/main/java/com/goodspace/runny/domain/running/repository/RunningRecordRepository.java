package com.goodspace.runny.domain.running.repository;

import com.goodspace.runny.domain.running.entity.RunningRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 러닝 기록 리포지토리. 멱등키 조회, 월별 히스토리, 어제 평균 페이스, 크루 주간 집계를 제공한다.
 */
public interface RunningRecordRepository extends JpaRepository<RunningRecord, Long> {

    Optional<RunningRecord> findByClientRunId(String clientRunId);

    Optional<RunningRecord> findByIdAndUserId(Long id, Long userId);

    /** 월별 기록 목록 (최신순) */
    List<RunningRecord> findByUserIdAndEndedAtBetweenOrderByEndedAtDesc(
            Long userId, LocalDateTime start, LocalDateTime end);

    /** 미확인 리포트 존재 여부 (홈 빨간 점) */
    boolean existsByUserIdAndCheckedFalse(Long userId);

    /** 어제 평균 페이스 (기록 없으면 null) - 넘치는 에너지 방출 업적 판정용 */
    @Query("SELECT AVG(r.avgPaceSec) FROM RunningRecord r " +
            "WHERE r.userId = :userId AND r.endedAt >= :start AND r.endedAt < :end")
    Double avgPaceBetween(@Param("userId") Long userId,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);

    /** 누적 러닝 거리 합계 - 누적 거리 단계 업적(벌써 이만큼이나?) 판정용 */
    @Query("SELECT COALESCE(SUM(r.distanceKm), 0) FROM RunningRecord r WHERE r.userId = :userId")
    double sumTotalDistance(@Param("userId") Long userId);

    /** 기간 내 러닝 거리 합계 - 지난주 총 거리(주간 퀘스트 target 스냅샷) 계산용 */
    @Query("SELECT COALESCE(SUM(r.distanceKm), 0) FROM RunningRecord r " +
            "WHERE r.userId = :userId AND r.endedAt >= :start AND r.endedAt < :end")
    double sumDistanceBetween(@Param("userId") Long userId,
                              @Param("start") LocalDateTime start,
                              @Param("end") LocalDateTime end);

    /**
     * 크루 주간 러닝 집계 (CrewRunningStatsProvider 실제 구현용).
     * 유저별 거리 합과 시간 합을 한 번에 반환해 주간 거리 랭킹, 이번 주 거리 합계,
     * 크루원 목록 지표를 모두 같은 스냅샷에서 조립한다.
     * 구간은 이번 주 월요일 00:00 이상 ~ 다음 주 월요일 00:00 미만(KST)이다.
     */
    @Query("SELECT r.userId, SUM(r.distanceKm), SUM(r.durationSec) FROM RunningRecord r " +
            "WHERE r.userId IN (SELECT m.userId FROM com.goodspace.runny.domain.crew.entity.CrewMember m " +
            "                   WHERE m.crewId = :crewId) " +
            "AND r.endedAt >= :weekStart AND r.endedAt < :weekEnd " +
            "GROUP BY r.userId")
    List<Object[]> aggregateWeeklyByCrew(@Param("crewId") Long crewId,
                                         @Param("weekStart") LocalDateTime weekStart,
                                         @Param("weekEnd") LocalDateTime weekEnd);
}
