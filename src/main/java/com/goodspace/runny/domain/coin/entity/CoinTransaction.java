package com.goodspace.runny.domain.coin.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 코인 원장 엔티티. 모든 적립(+)/차감(-)을 사유(type)와 참조 ID(ref_id)로 기록한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "coin_transaction")
public class CoinTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 적립은 양수, 차감은 음수
    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CoinTransactionType type;

    // 사유별 참조 대상 ID (예: DOG_PURCHASE면 breed_id, QUEST면 user_quest_id)
    @Column(name = "ref_id")
    private Long refId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public CoinTransaction(Long userId, int amount, CoinTransactionType type, Long refId) {
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.refId = refId;
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}
