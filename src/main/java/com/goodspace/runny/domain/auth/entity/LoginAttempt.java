package com.goodspace.runny.domain.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 로그인 실패 이력 엔티티. 이메일 단위로 실패 1건당 1행을 기록하며,
 * 10분 창 내 실패 횟수를 합산해 브루트포스 시도를 차단하는 근거가 된다. (Redis 없이 DB 기반)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "login_attempt", indexes = {
        @Index(name = "idx_login_attempt_email_created", columnList = "email, created_at")
})
public class LoginAttempt {

    private static final ZoneId ZONE_SEOUL = ZoneId.of("Asia/Seoul");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public LoginAttempt(String email) {
        this.email = email;
        this.createdAt = LocalDateTime.now(ZONE_SEOUL);
    }
}
