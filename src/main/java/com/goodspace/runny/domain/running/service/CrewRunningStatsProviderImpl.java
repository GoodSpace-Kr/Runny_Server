package com.goodspace.runny.domain.running.service;

import com.goodspace.runny.domain.crew.service.CrewRunningStatsProvider;
import com.goodspace.runny.domain.running.repository.RunningRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 크루 주간 러닝 집계 실제 구현 (크루 도메인 인터페이스 교체).
 * running_record를 크루원 기준으로 이번 주 월요일 00:00 (KST) 이후 집계해
 * 유저별 누적 거리/누적 시간/최단 평균 페이스를 한 번에 반환한다.
 */
@Component
@RequiredArgsConstructor
public class CrewRunningStatsProviderImpl implements CrewRunningStatsProvider {

    private final RunningRecordRepository runningRecordRepository;

    /** 이번 주 크루원별 러닝 집계 (거리 합 / 시간 합 / 최단 평균 페이스) */
    @Override
    public List<WeeklyStats> weeklyStats(Long crewId) {
        return runningRecordRepository
                .aggregateWeeklyByCrew(crewId, CrewRunningStatsProvider.weekStart())
                .stream()
                .map(row -> new WeeklyStats(
                        (Long) row[0],
                        ((Number) row[1]).doubleValue(),
                        ((Number) row[2]).longValue(),
                        ((Number) row[3]).longValue()))
                .toList();
    }
}
