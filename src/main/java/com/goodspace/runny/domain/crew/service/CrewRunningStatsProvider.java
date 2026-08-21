package com.goodspace.runny.domain.crew.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * 크루 주간 러닝 집계 제공자. 실제 구현은 러닝 도메인의 CrewRunningStatsProviderImpl이 담당해
 * 도메인 순환 참조를 피한다. 집계 구간은 이번 주 월요일 00:00부터 일요일 24:00까지(KST)이며,
 * 일요일이 끝나면 다음 월요일 00:00 기준으로 자동 초기화된다.
 * 크루 메인의 주간 거리 랭킹, 이번 주 거리 합계, 크루원 목록 지표에 함께 사용한다.
 */
public interface CrewRunningStatsProvider {

    /** 크루원 1명의 이번 주 러닝 집계 - 이번 주 기록이 없는 멤버는 이 객체 자체가 없다 */
    record WeeklyStats(Long userId, double distanceKm, long durationSec) {

        /** 주간 평균 페이스(초/km) - 총 시간 / 총 거리. 거리 0이면 0 */
        public long avgPaceSec() {
            return distanceKm > 0 ? Math.round(durationSec / distanceKm) : 0;
        }
    }

    /** 이번 주(월 00:00 ~ 일 24:00 KST) 크루원별 러닝 집계 - 기록이 없는 멤버는 목록에 포함되지 않는다 */
    List<WeeklyStats> weeklyStats(Long crewId);

    /** 이번 주 시작 시각 (월요일 00:00 KST) */
    static LocalDateTime weekStart() {
        return LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .toLocalDate()
                .atStartOfDay();
    }

    /** 이번 주 종료 시각 (다음 주 월요일 00:00 KST, 미포함 상한) */
    static LocalDateTime weekEnd() {
        return weekStart().plusDays(7);
    }
}
