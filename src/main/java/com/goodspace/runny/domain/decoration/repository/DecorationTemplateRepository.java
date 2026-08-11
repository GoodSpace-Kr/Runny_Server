package com.goodspace.runny.domain.decoration.repository;

import com.goodspace.runny.domain.decoration.entity.DecorationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 꾸미기 템플릿 리포지토리. 유저 단위 목록/개수 조회와 소유자 검증 조회를 제공한다.
 */
public interface DecorationTemplateRepository extends JpaRepository<DecorationTemplate, Long> {

    /** 내 템플릿 목록 (최신순) */
    List<DecorationTemplate> findByUserIdOrderByIdDesc(Long userId);

    /** 유저당 저장 개수 (상한 검증용) */
    int countByUserId(Long userId);

    /** 소유자 검증을 포함한 단건 조회 */
    Optional<DecorationTemplate> findByIdAndUserId(Long id, Long userId);
}
