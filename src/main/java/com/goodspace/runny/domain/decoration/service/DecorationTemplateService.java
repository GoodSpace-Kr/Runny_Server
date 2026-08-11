package com.goodspace.runny.domain.decoration.service;

import com.goodspace.runny.domain.decoration.dto.DecorationDto;
import com.goodspace.runny.domain.decoration.entity.DecorationTemplate;
import com.goodspace.runny.domain.decoration.repository.DecorationTemplateRepository;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 꾸미기 템플릿 서비스. 위젯 배치 레이아웃(JSON)만 저장/조회/삭제한다.
 * 서버는 layoutJson의 내용을 해석하지 않으며 파싱 가능 여부와 크기, 유저당 개수만 검증한다.
 */
@Service
@RequiredArgsConstructor
public class DecorationTemplateService {

    // 유저당 템플릿 저장 상한
    private static final int MAX_TEMPLATES_PER_USER = 10;
    // layoutJson 최대 크기 (바이트)
    private static final int MAX_LAYOUT_BYTES = 20 * 1024;
    private static final int MAX_NAME_LENGTH = 20;

    private final DecorationTemplateRepository decorationTemplateRepository;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /** 내 템플릿 목록 (최신순) */
    @Transactional(readOnly = true)
    public List<DecorationDto.TemplateItem> getTemplates(Long userId) {
        return decorationTemplateRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(DecorationDto.TemplateItem::from)
                .toList();
    }

    /** 템플릿 저장 - 개수 상한/크기/JSON 형식 검증 후 저장. 이름 미입력 시 "템플릿 N"으로 자동 부여 */
    @Transactional
    public DecorationDto.TemplateItem save(Long userId, DecorationDto.TemplateSaveRequest request) {
        int count = decorationTemplateRepository.countByUserId(userId);
        if (count >= MAX_TEMPLATES_PER_USER) {
            throw new BusinessException(ErrorCode.DECORATION_001);
        }
        validateLayout(request.layoutJson());

        String name = (request.name() == null || request.name().isBlank())
                ? "템플릿 " + (count + 1)
                : request.name().trim();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.DECORATION_004);
        }
        DecorationTemplate saved = decorationTemplateRepository
                .save(new DecorationTemplate(userId, name, request.layoutJson()));
        return DecorationDto.TemplateItem.from(saved);
    }

    /** 템플릿 삭제 - 본인 소유만 */
    @Transactional
    public void delete(Long userId, Long templateId) {
        DecorationTemplate template = decorationTemplateRepository.findByIdAndUserId(templateId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DECORATION_005));
        decorationTemplateRepository.delete(template);
    }

    /** 회원 탈퇴 훅 - 저장한 템플릿 전부 삭제 */
    @Transactional
    public void deleteAllOf(Long userId) {
        decorationTemplateRepository.deleteAll(decorationTemplateRepository.findByUserIdOrderByIdDesc(userId));
    }

    /** 레이아웃 JSON 검증 - 크기 상한과 파싱 가능 여부만 확인하고 내용은 해석하지 않는다 */
    private void validateLayout(String layoutJson) {
        if (layoutJson.getBytes(StandardCharsets.UTF_8).length > MAX_LAYOUT_BYTES) {
            throw new BusinessException(ErrorCode.DECORATION_002);
        }
        try {
            jsonMapper.readTree(layoutJson);
        } catch (RuntimeException e) {
            // Jackson 3의 예외는 RuntimeException 계열이므로 IOException으로 잡히지 않는다
            throw new BusinessException(ErrorCode.DECORATION_003);
        }
    }
}
