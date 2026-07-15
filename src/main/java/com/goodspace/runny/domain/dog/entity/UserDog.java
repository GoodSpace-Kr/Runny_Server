package com.goodspace.runny.domain.dog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 보유 강아지 엔티티. 경험치/레벨/스탯은 계정이 아닌 강아지 단위로 관리한다.
 * 레벨은 1레벨당 1000xp 고정, 만렙 50(초과 경험치 미적립).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_dog")
public class UserDog {

    public static final int EXP_PER_LEVEL = 1000;
    public static final int MAX_LEVEL = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breed_id", nullable = false)
    private DogBreed breed;

    @Column(nullable = false, length = 7)
    private String name;

    @Column(nullable = false)
    private int level;

    // 현재 레벨 내 경험치 (0 ~ 999)
    @Column(nullable = false)
    private int exp;

    @Column(nullable = false)
    private int stamina;

    @Column(nullable = false)
    private int endurance;

    @Column(nullable = false)
    private int speed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public UserDog(Long userId, DogBreed breed, String name) {
        this.userId = userId;
        this.breed = breed;
        this.name = name;
        this.level = 1;
        this.exp = 0;
        this.stamina = 0;
        this.endurance = 0;
        this.speed = 0;
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    /**
     * 경험치 적립 + 레벨업 처리. 만렙(50)이면 적립하지 않는다.
     * 변화 로그 기록을 위해 적용 전후 레벨과 실제 반영된 경험치를 결과로 반환한다.
     */
    public ExpResult addExp(int amount) {
        int levelBefore = this.level;
        if (this.level >= MAX_LEVEL || amount <= 0) {
            return new ExpResult(levelBefore, this.level, 0);
        }
        int applied = amount;
        this.exp += amount;
        while (this.exp >= EXP_PER_LEVEL && this.level < MAX_LEVEL) {
            this.exp -= EXP_PER_LEVEL;
            this.level++;
        }
        // 만렙 도달 시 초과 경험치는 버린다
        if (this.level >= MAX_LEVEL) {
            applied = amount - this.exp;
            this.exp = 0;
        }
        return new ExpResult(levelBefore, this.level, applied);
    }

    /** 러닝 완료 시 스탯 증가 반영 (계산은 StatCalculator 담당) */
    public void addStats(int staminaDelta, int enduranceDelta, int speedDelta) {
        this.stamina += staminaDelta;
        this.endurance += enduranceDelta;
        this.speed += speedDelta;
    }

    /** 성장 단계 파생 조회 */
    public GrowthStage growthStage() {
        return GrowthStage.fromLevel(this.level);
    }

    /** 다음 레벨까지 필요한 경험치 (만렙이면 0) */
    public int expToNextLevel() {
        return this.level >= MAX_LEVEL ? 0 : EXP_PER_LEVEL - this.exp;
    }

    /** 경험치 적립 결과 - 변화 로그(dog_change_log) 기록용 */
    public record ExpResult(int levelBefore, int levelAfter, int appliedExp) {
        public boolean leveledUp() {
            return levelAfter > levelBefore;
        }
    }
}
