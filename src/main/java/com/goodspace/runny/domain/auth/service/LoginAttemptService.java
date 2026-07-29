package com.goodspace.runny.domain.auth.service;

import com.goodspace.runny.domain.auth.entity.LoginAttempt;
import com.goodspace.runny.domain.auth.repository.LoginAttemptRepository;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 로그인 브루트포스 방어 서비스. 동일 이메일 10분 내 실패 5회 초과 시 로그인을 차단한다.
 * 이메일 인증코드의 Attempt Limit과 동일하게 Redis 없이 DB 테이블(login_attempt)로 판정한다.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");
    // 10분 창 내 최대 로그인 실패 허용 횟수
    private static final int LIMIT_WINDOW_MINUTES = 10;
    private static final int MAX_FAIL_COUNT = 5;
    // 정리 대상 기준 - 판정 창보다 넉넉하게 하루 지난 이력은 삭제
    private static final int CLEANUP_HOURS = 24;

    private final LoginAttemptRepository loginAttemptRepository;

    /** 로그인 시도 전 차단 여부 검사 - 10분 내 실패 5회 초과 시 AUTH_017 */
    @Transactional(readOnly = true)
    public void checkNotBlocked(String email) {
        LocalDateTime windowStart = LocalDateTime.now(ZONE_SEOUL).minusMinutes(LIMIT_WINDOW_MINUTES);
        if (loginAttemptRepository.countByEmailAndCreatedAtAfter(email, windowStart) >= MAX_FAIL_COUNT) {
            throw new BusinessException(ErrorCode.AUTH_017);
        }
    }

    /**
     * 로그인 실패 기록. 호출부(AuthService.login)가 실패 예외로 롤백되더라도
     * 실패 이력은 반드시 남아야 하므로 별도 트랜잭션(REQUIRES_NEW)으로 커밋한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String email) {
        loginAttemptRepository.save(new LoginAttempt(email));
        // 판정에 불필요한 오래된 이력을 같은 시점에 정리 (별도 스케줄러 없이 자연 정리)
        loginAttemptRepository.deleteAllCreatedBefore(
                LocalDateTime.now(ZONE_SEOUL).minusHours(CLEANUP_HOURS));
    }

    /** 로그인 성공 시 실패 카운터 초기화 */
    @Transactional
    public void clear(String email) {
        loginAttemptRepository.deleteByEmail(email);
    }
}
