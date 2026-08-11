package com.goodspace.runny.domain.running.service;

import com.goodspace.runny.domain.achievement.service.TotalDistanceProvider;
import com.goodspace.runny.domain.running.repository.RunningRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 누적 러닝 거리 실제 구현 (업적 도메인 인터페이스 교체).
 * 업적 목록의 누적 거리 단계 업적 진행 바 표시에 사용한다.
 */
@Component
@RequiredArgsConstructor
public class TotalDistanceProviderImpl implements TotalDistanceProvider {

    private final RunningRecordRepository runningRecordRepository;

    /** 유저의 전체 러닝 거리 합계 (km) */
    @Override
    public double totalDistanceKm(Long userId) {
        return runningRecordRepository.sumTotalDistance(userId);
    }
}
