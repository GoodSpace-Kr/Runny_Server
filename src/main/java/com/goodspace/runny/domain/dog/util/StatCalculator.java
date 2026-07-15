package com.goodspace.runny.domain.dog.util;

/**
 * 러닝 완료 시 스탯 증가 계산기 (기획 문서 4.B 공식).
 * 체력 = 거리 원점수(보정 없음), 지구력 = 시간 원점수 x 페이스 보정계수(반올림),
 * 스피드 = 페이스 원점수 x 거리 보정계수(반올림). 보정계수 바닥값은 0.35.
 * 러닝 도메인(9단계)에서 호출한다.
 */
public final class StatCalculator {

    // 지구력의 페이스 보정 기준: 8분/km = 480초 (일반 대중 러닝 최저 기준선)
    private static final double BASE_PACE_SEC = 480.0;
    // 스피드의 거리 보정 기준: 3km (이 정도는 되어야 페이스가 유의미)
    private static final double BASE_DISTANCE_KM = 3.0;
    // 보정계수 바닥값
    private static final double FLOOR = 0.35;

    private StatCalculator() {
    }

    /** 스탯 증가량 계산 결과 */
    public record StatDelta(int stamina, int endurance, int speed) {
    }

    /**
     * 스탯 증가량 일괄 계산.
     * @param distanceKm 총 거리(km), @param durationSec 총 시간(초), @param avgPaceSec 평균 페이스(초/km)
     */
    public static StatDelta calculate(double distanceKm, long durationSec, long avgPaceSec) {
        int stamina = staminaScore(distanceKm);
        int endurance = (int) Math.round(durationScore(durationSec) * paceFactor(avgPaceSec));
        int speed = (int) Math.round(paceScore(avgPaceSec) * distanceFactor(distanceKm));
        return new StatDelta(stamina, endurance, speed);
    }

    /** 체력 - 총 거리 기준 원점수 (2km 미만 1 / 2~4 2 / 4~6 3 / 6~8 4 / 8 이상 5) */
    static int staminaScore(double distanceKm) {
        if (distanceKm < 2) {
            return 1;
        }
        if (distanceKm < 4) {
            return 2;
        }
        if (distanceKm < 6) {
            return 3;
        }
        if (distanceKm < 8) {
            return 4;
        }
        return 5;
    }

    /** 지구력 - 러닝 시간 원점수 (10분 미만 1 / 10~20 2 / 20~35 3 / 35~50 4 / 50 이상 5) */
    static int durationScore(long durationSec) {
        long minutes = durationSec / 60;
        if (minutes < 10) {
            return 1;
        }
        if (minutes < 20) {
            return 2;
        }
        if (minutes < 35) {
            return 3;
        }
        if (minutes < 50) {
            return 4;
        }
        return 5;
    }

    /** 스피드 - 페이스 원점수 (8:00 초과 1 / 7:00~8:00 2 / 6:00~7:00 3 / 5:00~6:00 4 / 5:00 미만 5) */
    static int paceScore(long avgPaceSec) {
        if (avgPaceSec > 480) {
            return 1;
        }
        if (avgPaceSec > 420) {
            return 2;
        }
        if (avgPaceSec > 360) {
            return 3;
        }
        if (avgPaceSec >= 300) {
            return 4;
        }
        return 5;
    }

    /** 페이스 보정계수 = max(0.35, min(1, 480 / 실제페이스)). 15분/km 이상은 바닥값 0.35 */
    static double paceFactor(long avgPaceSec) {
        if (avgPaceSec <= 0) {
            return FLOOR;
        }
        return Math.max(FLOOR, Math.min(1.0, BASE_PACE_SEC / avgPaceSec));
    }

    /** 거리 보정계수 = max(0.35, min(1, 실제거리 / 3km)) */
    static double distanceFactor(double distanceKm) {
        return Math.max(FLOOR, Math.min(1.0, distanceKm / BASE_DISTANCE_KM));
    }
}
