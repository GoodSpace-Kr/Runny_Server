package com.goodspace.runny.domain.coin.entity;

/**
 * 코인 증감 사유 구분. 모든 증감은 원장에 사유와 함께 기록된다.
 */
public enum CoinTransactionType {
    QUEST, ACHIEVEMENT, ITEM_PURCHASE, DOG_PURCHASE, CREW_SPEND, CHARGE
}
