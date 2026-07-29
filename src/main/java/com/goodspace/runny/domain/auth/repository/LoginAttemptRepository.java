package com.goodspace.runny.domain.auth.repository;

import com.goodspace.runny.domain.auth.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

/**
 * 로그인 실패 이력 리포지토리. 10분 창 기준 실패 횟수 조회와
 * 로그인 성공/오래된 이력 정리를 제공한다.
 */
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    /** 기준 시각 이후 실패 횟수 - 브루트포스 차단(10분 5회) 판정 */
    int countByEmailAndCreatedAtAfter(String email, LocalDateTime since);

    /** 로그인 성공 시 해당 이메일의 실패 이력 전체 삭제 (카운터 초기화 효과) */
    @Modifying
    @Query("DELETE FROM LoginAttempt a WHERE a.email = :email")
    void deleteByEmail(@Param("email") String email);

    /** 오래된 실패 이력 정리 - 판정 창을 벗어난 행은 의미가 없으므로 주기적으로 삭제 */
    @Modifying
    @Query("DELETE FROM LoginAttempt a WHERE a.createdAt < :before")
    void deleteAllCreatedBefore(@Param("before") LocalDateTime before);
}
