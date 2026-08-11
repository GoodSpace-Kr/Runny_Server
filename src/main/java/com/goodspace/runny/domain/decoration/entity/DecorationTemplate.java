package com.goodspace.runny.domain.decoration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 히스토리 꾸미기 템플릿 엔티티. 사용자가 저장한 위젯 배치 레이아웃만 보관한다.
 * 유저당 슬롯 3칸(1~3) 고정이며 (user_id, slot) UNIQUE로 한 칸에 하나만 저장되도록 강제한다.
 * layout_json의 스키마는 프론트가 소유하며 서버는 내용을 해석하지 않는다(JSON 파싱 가능 여부와 크기만 검증).
 * 배경 사진은 저장하지 않고, 지표 값은 템플릿 적용 시점의 러닝 기록으로 프론트가 채운다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "decoration_template", uniqueConstraints = {
        @UniqueConstraint(name = "uk_decoration_template_user_slot", columnNames = {"user_id", "slot"})
})
public class DecorationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 저장 칸 번호 (1~3 고정)
    @Column(nullable = false)
    private int slot;

    // 템플릿 이름 (미입력 시 서비스에서 "템플릿 {slot}"으로 자동 부여)
    @Column(length = 20)
    private String name;

    // 위젯 배치 레이아웃 JSON (프론트 스키마, 서버는 해석하지 않음)
    @Column(name = "layout_json", nullable = false, columnDefinition = "TEXT")
    private String layoutJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public DecorationTemplate(Long userId, int slot, String name, String layoutJson) {
        this.userId = userId;
        this.slot = slot;
        this.name = name;
        this.layoutJson = layoutJson;
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}
