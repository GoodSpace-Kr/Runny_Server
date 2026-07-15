package com.goodspace.runny.domain.dog.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * StatCalculator 단위 테스트. 기획 문서 4.B의 원점수 표, 보정계수 예시, 바닥값을 검증한다.
 */
class StatCalculatorTest {

    @Test
    @DisplayName("체력 원점수 - 거리 구간 경계 검증")
    void staminaScore() {
        assertEquals(1, StatCalculator.staminaScore(1.99));
        assertEquals(2, StatCalculator.staminaScore(2.0));
        assertEquals(2, StatCalculator.staminaScore(3.99));
        assertEquals(3, StatCalculator.staminaScore(4.0));
        assertEquals(4, StatCalculator.staminaScore(6.0));
        assertEquals(5, StatCalculator.staminaScore(8.0));
        assertEquals(5, StatCalculator.staminaScore(15.0));
    }

    @Test
    @DisplayName("지구력 시간 원점수 - 분 구간 경계 검증")
    void durationScore() {
        assertEquals(1, StatCalculator.durationScore(9 * 60 + 59));
        assertEquals(2, StatCalculator.durationScore(10 * 60));
        assertEquals(3, StatCalculator.durationScore(20 * 60));
        assertEquals(4, StatCalculator.durationScore(35 * 60));
        assertEquals(5, StatCalculator.durationScore(50 * 60));
    }

    @Test
    @DisplayName("페이스 보정계수 - 문서 예시값 검증 (8:00->1.0, 9:00->0.89, 10:00->0.80, 12:00->0.67, 15:00 이상->0.35)")
    void paceFactor() {
        assertEquals(1.0, StatCalculator.paceFactor(480), 0.001);
        assertEquals(0.889, StatCalculator.paceFactor(540), 0.001);
        assertEquals(0.8, StatCalculator.paceFactor(600), 0.001);
        assertEquals(0.667, StatCalculator.paceFactor(720), 0.001);
        assertEquals(0.35, StatCalculator.paceFactor(900), 0.001);
        assertEquals(0.35, StatCalculator.paceFactor(1200), 0.001);
        // 8:00/km보다 빠르면 상한 1.0 유지
        assertEquals(1.0, StatCalculator.paceFactor(300), 0.001);
    }

    @Test
    @DisplayName("거리 보정계수 - 문서 예시값 검증 (3km 이상->1.0, 2km->0.67, 1km->바닥값 0.35)")
    void distanceFactor() {
        assertEquals(1.0, StatCalculator.distanceFactor(3.0), 0.001);
        assertEquals(1.0, StatCalculator.distanceFactor(10.0), 0.001);
        assertEquals(0.667, StatCalculator.distanceFactor(2.0), 0.001);
        // 1km는 원래 0.33이지만 바닥값 0.35 적용
        assertEquals(0.35, StatCalculator.distanceFactor(1.0), 0.001);
        assertEquals(0.35, StatCalculator.distanceFactor(0.5), 0.001);
    }

    @Test
    @DisplayName("스피드 페이스 원점수 - 구간 경계 검증")
    void paceScore() {
        assertEquals(1, StatCalculator.paceScore(481));
        assertEquals(2, StatCalculator.paceScore(480));
        assertEquals(2, StatCalculator.paceScore(421));
        assertEquals(3, StatCalculator.paceScore(420));
        assertEquals(3, StatCalculator.paceScore(361));
        assertEquals(4, StatCalculator.paceScore(360));
        assertEquals(4, StatCalculator.paceScore(300));
        assertEquals(5, StatCalculator.paceScore(299));
    }

    @Test
    @DisplayName("종합 - 5km / 30분 / 6:00 페이스 러닝의 스탯 증가량")
    void calculateTypicalRun() {
        // 체력: 5km -> 3 / 지구력: 30분 원점수 3 x 보정 1.0 = 3 / 스피드: 360초 원점수 4 x 거리보정 1.0 = 4
        StatCalculator.StatDelta delta = StatCalculator.calculate(5.0, 30 * 60, 360);
        assertEquals(3, delta.stamina());
        assertEquals(3, delta.endurance());
        assertEquals(4, delta.speed());
    }

    @Test
    @DisplayName("종합 - 1km / 12분 / 12:00 페이스 느린 짧은 러닝 (보정계수 바닥/축소 적용)")
    void calculateSlowShortRun() {
        // 체력: 1km -> 1 / 지구력: 12분 원점수 2 x 보정 0.667 = 1.33 -> 반올림 1
        // 스피드: 720초 원점수 1 x 거리보정 0.35 = 0.35 -> 반올림 0
        StatCalculator.StatDelta delta = StatCalculator.calculate(1.0, 12 * 60, 720);
        assertEquals(1, delta.stamina());
        assertEquals(1, delta.endurance());
        assertEquals(0, delta.speed());
    }
}
