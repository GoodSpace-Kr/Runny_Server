package com.goodspace.runny.domain.achievement.service;

/**
 * 유저 누적 러닝 거리 제공자 인터페이스.
 * 누적 거리 단계 업적("벌써 이만큼이나?")의 진행 바 계산에 사용하며,
 * 실제 구현은 러닝 도메인(TotalDistanceProviderImpl)이 담당해 도메인 순환 참조를 피한다.
 */
public interface TotalDistanceProvider {

    /** 유저의 전체 러닝 거리 합계 (km, 기록 없으면 0) */
    double totalDistanceKm(Long userId);
}
