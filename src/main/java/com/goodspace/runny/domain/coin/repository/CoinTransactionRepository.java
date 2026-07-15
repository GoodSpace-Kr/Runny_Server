package com.goodspace.runny.domain.coin.repository;

import com.goodspace.runny.domain.coin.entity.CoinTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 코인 원장 리포지토리. 내역 조회는 8단계(GET /api/coins/transactions)에서 사용한다.
 */
public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, Long> {

    Page<CoinTransaction> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);
}
