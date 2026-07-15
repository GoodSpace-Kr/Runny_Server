package com.goodspace.runny.domain.dog.service;

import com.goodspace.runny.domain.dog.entity.ChangeSource;
import com.goodspace.runny.domain.dog.entity.DogChangeLog;
import com.goodspace.runny.domain.dog.entity.UserDog;
import com.goodspace.runny.domain.dog.repository.DogChangeLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 강아지 변화 로그 공용 서비스. 경험치/스탯이 변하는 모든 지점(러닝 완료, 퀘스트/업적 보상 수령)에서
 * 기록하고, 펫 프로필의 미확인 변화 요약과 빨간 점 판정을 제공한다.
 */
@Service
@RequiredArgsConstructor
public class DogChangeLogService {

    private final DogChangeLogRepository dogChangeLogRepository;

    /** 경험치 변화 기록 (퀘스트/업적 보상 수령 시 - 레벨업 정보 포함) */
    @Transactional
    public void recordExpChange(Long userDogId, ChangeSource source, UserDog.ExpResult result) {
        if (result.appliedExp() <= 0 && !result.leveledUp()) {
            return;
        }
        dogChangeLogRepository.save(DogChangeLog.builder()
                .userDogId(userDogId)
                .source(source)
                .expDelta(result.appliedExp())
                .levelBefore(result.levelBefore())
                .levelAfter(result.levelAfter())
                .build());
    }

    /** 스탯 변화 기록 (러닝 완료 시 - 9단계에서 호출) */
    @Transactional
    public void recordStatChange(Long userDogId, int staminaDelta, int enduranceDelta, int speedDelta, int level) {
        if (staminaDelta == 0 && enduranceDelta == 0 && speedDelta == 0) {
            return;
        }
        dogChangeLogRepository.save(DogChangeLog.builder()
                .userDogId(userDogId)
                .source(ChangeSource.RUNNING)
                .staminaDelta(staminaDelta)
                .enduranceDelta(enduranceDelta)
                .speedDelta(speedDelta)
                .levelBefore(level)
                .levelAfter(level)
                .build());
    }

    /** 미확인 변화 요약 생성 + 해당 로그 일괄 확인 처리 (펫 프로필 조회 시점) */
    @Transactional
    public ChangeSummary summarizeAndMarkSeen(Long userDogId) {
        List<DogChangeLog> unseen = dogChangeLogRepository.findByUserDogIdAndSeenFalseOrderByIdAsc(userDogId);
        if (unseen.isEmpty()) {
            return ChangeSummary.empty();
        }
        int expGained = unseen.stream().mapToInt(DogChangeLog::getExpDelta).sum();
        int staminaGained = unseen.stream().mapToInt(DogChangeLog::getStaminaDelta).sum();
        int enduranceGained = unseen.stream().mapToInt(DogChangeLog::getEnduranceDelta).sum();
        int speedGained = unseen.stream().mapToInt(DogChangeLog::getSpeedDelta).sum();
        int levelBefore = unseen.get(0).getLevelBefore();
        int levelAfter = unseen.get(unseen.size() - 1).getLevelAfter();

        dogChangeLogRepository.markAllSeen(userDogId);
        return new ChangeSummary(true, expGained, levelBefore, levelAfter,
                staminaGained, enduranceGained, speedGained);
    }

    /** 미확인 변경 존재 여부 - 놀이터 응답의 hasDogProfileBadge에 사용 (친구/놀이터 단계에서 호출 예정) */
    @Transactional(readOnly = true)
    public boolean hasUnseenChanges(Long userDogId) {
        return dogChangeLogRepository.existsByUserDogIdAndSeenFalse(userDogId);
    }

    /** 미확인 변화 요약 - 경험치 총 증가, 레벨 before/after, 스탯 증가량 */
    public record ChangeSummary(
            boolean hasChanges,
            int expGained,
            int levelBefore,
            int levelAfter,
            int staminaGained,
            int enduranceGained,
            int speedGained
    ) {
        public static ChangeSummary empty() {
            return new ChangeSummary(false, 0, 0, 0, 0, 0, 0);
        }
    }
}
