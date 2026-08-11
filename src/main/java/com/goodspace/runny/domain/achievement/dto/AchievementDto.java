package com.goodspace.runny.domain.achievement.dto;

import com.goodspace.runny.domain.achievement.entity.Achievement;
import com.goodspace.runny.domain.achievement.entity.RewardType;
import com.goodspace.runny.domain.achievement.entity.UserAchievement;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 업적 요청/응답 DTO 모음.
 */
public final class AchievementDto {

    private AchievementDto() {
    }

    /**
     * 업적 항목 - 아이콘/보상/달성/수령 여부 + 진행도. 프론트 필터(전체·달성·미달성)는 achieved로 처리하고,
     * 진행 바는 progress/targetValue로 그린다(1회성 업적은 target 1, 달성 시 progress 1).
     */
    public record Item(
            Long achievementId,
            String code,
            String title,
            String description,
            String imageUrl,
            RewardType rewardType,
            int rewardCoin,
            boolean achieved,
            boolean claimed,
            double progress,
            double targetValue,
            LocalDateTime achievedAt
    ) {
        public static Item of(Achievement achievement, UserAchievement mine, double progress, double targetValue) {
            boolean achieved = mine != null;
            // 달성한 업적은 진행 바를 가득 채운다 (카운터가 목표 미만으로 남아 있는 경우 방어)
            double displayProgress = achieved ? targetValue : Math.min(progress, targetValue);
            return new Item(
                    achievement.getId(), achievement.getCode(), achievement.getTitle(),
                    achievement.getDescription(), achievement.getImageUrl(),
                    achievement.getRewardType(), achievement.getRewardCoin(),
                    achieved, achieved && mine.isClaimed(),
                    displayProgress, targetValue,
                    mine == null ? null : mine.getAchievedAt()
            );
        }
    }

    /** 업적 목록 응답 - 달성률 포함 */
    public record ListResponse(
            List<Item> achievements,
            int achievedCount,
            int totalCount,
            double achievementRate
    ) {
    }

    /** 러닝 완료 시 새로 달성된 업적 (알림성 응답 - 9단계 러닝 완료 응답에 포함) */
    public record AchievedItem(
            Long achievementId,
            String code,
            String title,
            String imageUrl
    ) {
    }

    /** 보상 수령 응답 - 견종 해금형은 unlockedBreedId로 입양 화면 연결 */
    public record ClaimResponse(
            RewardType rewardType,
            int grantedCoin,
            Long unlockedBreedId
    ) {
    }
}
