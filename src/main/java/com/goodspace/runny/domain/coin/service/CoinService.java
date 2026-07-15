package com.goodspace.runny.domain.coin.service;

import com.goodspace.runny.domain.coin.entity.CoinTransaction;
import com.goodspace.runny.domain.coin.entity.CoinTransactionType;
import com.goodspace.runny.domain.coin.repository.CoinTransactionRepository;
import com.goodspace.runny.domain.user.repository.UserRepository;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 코인 증감 공통 서비스. 모든 도메인의 코인 차감/적립은 반드시 이 서비스를 거친다.
 * 차감은 조건부 UPDATE(coin >= :amount)로 DB가 원자적으로 잔액을 보장하며, 락 대기가 없어 데드락 위험이 낮다.
 * 증감 직후 같은 트랜잭션에서 coin_transaction 원장을 기록한다.
 */
@Service
@RequiredArgsConstructor
public class CoinService {

    private final UserRepository userRepository;
    private final CoinTransactionRepository coinTransactionRepository;

    /** 코인 차감 - 조건부 UPDATE 영향 행이 0이면 잔액 부족(COIN_001). 호출측 트랜잭션에 참여 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void deduct(Long userId, int amount, CoinTransactionType type, Long refId) {
        validateAmount(amount);
        int affected = userRepository.deductCoin(userId, amount);
        if (affected == 0) {
            throw new BusinessException(ErrorCode.COIN_001);
        }
        coinTransactionRepository.save(new CoinTransaction(userId, -amount, type, refId));
    }

    /** 코인 적립 - UPDATE 방식 가산 후 원장 기록. 호출측 트랜잭션에 참여 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void add(Long userId, int amount, CoinTransactionType type, Long refId) {
        validateAmount(amount);
        int affected = userRepository.addCoin(userId, amount);
        if (affected == 0) {
            throw new BusinessException(ErrorCode.USER_003);
        }
        coinTransactionRepository.save(new CoinTransaction(userId, amount, type, refId));
    }

    /** 증감 금액은 항상 양수여야 한다 */
    private void validateAmount(int amount) {
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.COIN_002);
        }
    }
}
