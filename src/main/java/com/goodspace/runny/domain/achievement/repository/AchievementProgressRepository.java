package com.goodspace.runny.domain.achievement.repository;

import com.goodspace.runny.domain.achievement.entity.AchievementProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 업적 진행 카운터 리포지토리.
 */
public interface AchievementProgressRepository extends JpaRepository<AchievementProgress, Long> {

    Optional<AchievementProgress> findByUserIdAndCode(Long userId, String code);

    /** 유저의 전체 진행 카운터 (업적 목록 진행 바 일괄 조립용) */
    List<AchievementProgress> findByUserId(Long userId);
}
