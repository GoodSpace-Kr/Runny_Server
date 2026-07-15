package com.goodspace.runny.domain.dog.entity;

/**
 * 강아지 성장 단계. 레벨에서 파생된다: 퍼피(1~9) / 청소년기(10~24) / 성년기(25~49) / 만렙(50).
 */
public enum GrowthStage {
    PUPPY, JUNIOR, ADULT, MAX;

    /** 레벨로 성장 단계 판정 */
    public static GrowthStage fromLevel(int level) {
        if (level >= 50) {
            return MAX;
        }
        if (level >= 25) {
            return ADULT;
        }
        if (level >= 10) {
            return JUNIOR;
        }
        return PUPPY;
    }
}
