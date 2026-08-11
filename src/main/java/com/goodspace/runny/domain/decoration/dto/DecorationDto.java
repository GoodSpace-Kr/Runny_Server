package com.goodspace.runny.domain.decoration.dto;

import com.goodspace.runny.domain.decoration.entity.DecorationTemplate;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * 꾸미기 템플릿 요청/응답 DTO 모음.
 */
public final class DecorationDto {

    private DecorationDto() {
    }

    /** 템플릿 저장 요청 - layoutJson은 위젯 배치 정보(JSON 문자열). name 미입력 시 자동 부여 */
    public record TemplateSaveRequest(
            String name,
            @NotBlank String layoutJson
    ) {
    }

    /** 템플릿 응답 - 프론트가 layoutJson을 파싱해 위젯을 배치하고 값은 현재 러닝 기록으로 채운다 */
    public record TemplateItem(
            Long templateId,
            String name,
            String layoutJson,
            LocalDateTime createdAt
    ) {
        public static TemplateItem from(DecorationTemplate template) {
            return new TemplateItem(template.getId(), template.getName(),
                    template.getLayoutJson(), template.getCreatedAt());
        }
    }
}
