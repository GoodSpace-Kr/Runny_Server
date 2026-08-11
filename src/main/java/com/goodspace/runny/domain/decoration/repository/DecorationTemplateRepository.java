package com.goodspace.runny.domain.decoration.repository;

import com.goodspace.runny.domain.decoration.entity.DecorationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 꾸미기 템플릿 리포지토리. 유저의 슬롯 3칸 조회와 칸 단위 조회/삭제를 제공한다.
 */
public interface DecorationTemplateRepository extends JpaRepository<DecorationTemplate, Long> {

    /** 내 템플릿 전체 (슬롯 번호 오름차순) - 빈 칸은 조회 결과에 없으므로 서비스에서 채운다 */
    List<DecorationTemplate> findByUserIdOrderBySlotAsc(Long userId);

    /** 특정 칸의 템플릿 (없으면 빈 칸) */
    Optional<DecorationTemplate> findByUserIdAndSlot(Long userId, int slot);
}
