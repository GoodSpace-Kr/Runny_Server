package com.goodspace.runny.domain.dog.service;

import org.springframework.stereotype.Component;

/**
 * 업적 해금형 견종의 해금 여부 판정 인터페이스.
 * 업적 도메인은 8단계 구현이므로 지금은 항상 미해금(false)을 반환하는 기본 구현을 두고,
 * 8단계에서 user_achievement를 조회하는 실제 구현으로 교체한다.
 */
public interface AchievementUnlockChecker {

    /** 해당 업적 코드를 유저가 달성(해금)했는지 여부 */
    boolean isUnlocked(Long userId, String achievementCode);

    /** 기본 구현 - 업적 도메인 구현 전까지 항상 미해금 처리 */
    @Component
    class NotYetImplemented implements AchievementUnlockChecker {
        @Override
        public boolean isUnlocked(Long userId, String achievementCode) {
            // TODO(8단계): user_achievement 조회 기반 실제 구현으로 교체
            return false;
        }
    }
}
