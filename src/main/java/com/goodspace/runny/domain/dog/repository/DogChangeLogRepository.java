package com.goodspace.runny.domain.dog.repository;

import com.goodspace.runny.domain.dog.entity.DogChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 강아지 변화 로그 리포지토리. 미확인 로그 조회/일괄 확인 처리를 제공한다.
 */
public interface DogChangeLogRepository extends JpaRepository<DogChangeLog, Long> {

    List<DogChangeLog> findByUserDogIdAndSeenFalseOrderByIdAsc(Long userDogId);

    boolean existsByUserDogIdAndSeenFalse(Long userDogId);

    /** 미확인 로그 일괄 확인 처리 (프로필 조회 시점) */
    @Modifying
    @Query("UPDATE DogChangeLog l SET l.seen = true WHERE l.userDogId = :userDogId AND l.seen = false")
    int markAllSeen(@Param("userDogId") Long userDogId);
}
