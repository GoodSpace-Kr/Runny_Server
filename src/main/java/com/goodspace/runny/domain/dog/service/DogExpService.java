package com.goodspace.runny.domain.dog.service;

import com.goodspace.runny.domain.dog.entity.ChangeSource;
import com.goodspace.runny.domain.dog.entity.UserDog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 강아지 경험치 적립 공용 서비스. addExp 호출 시 dog_change_log 기록이 자동으로 연결된다.
 * 퀘스트/업적 보상 수령(8단계)에서 이 서비스를 통해 경험치를 지급한다.
 */
@Service
@RequiredArgsConstructor
public class DogExpService {

    private final DogChangeLogService dogChangeLogService;

    /** 경험치 적립 + 레벨업 처리 + 변화 로그 자동 기록. 호출측 트랜잭션에 참여 */
    @Transactional(propagation = Propagation.MANDATORY)
    public UserDog.ExpResult addExp(UserDog dog, int amount, ChangeSource source) {
        UserDog.ExpResult result = dog.addExp(amount);
        dogChangeLogService.recordExpChange(dog.getId(), source, result);
        return result;
    }
}
