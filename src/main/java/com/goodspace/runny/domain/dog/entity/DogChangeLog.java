package com.goodspace.runny.domain.dog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 강아지 변화 로그 엔티티. 경험치/스탯이 변하는 모든 지점에서 기록되는 원장이며,
 * 펫 프로필의 미확인 변화 요약과 놀이터 프로필 버튼 빨간 점(hasDogProfileBadge)의 근거가 된다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "dog_change_log")
public class DogChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_dog_id", nullable = false)
    private Long userDogId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChangeSource source;

    @Column(name = "exp_delta", nullable = false)
    private int expDelta;

    @Column(name = "stamina_delta", nullable = false)
    private int staminaDelta;

    @Column(name = "endurance_delta", nullable = false)
    private int enduranceDelta;

    @Column(name = "speed_delta", nullable = false)
    private int speedDelta;

    @Column(name = "level_before", nullable = false)
    private int levelBefore;

    @Column(name = "level_after", nullable = false)
    private int levelAfter;

    @Column(name = "is_seen", nullable = false)
    private boolean seen;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private DogChangeLog(Long userDogId, ChangeSource source, int expDelta,
                         int staminaDelta, int enduranceDelta, int speedDelta,
                         int levelBefore, int levelAfter) {
        this.userDogId = userDogId;
        this.source = source;
        this.expDelta = expDelta;
        this.staminaDelta = staminaDelta;
        this.enduranceDelta = enduranceDelta;
        this.speedDelta = speedDelta;
        this.levelBefore = levelBefore;
        this.levelAfter = levelAfter;
        this.seen = false;
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}
