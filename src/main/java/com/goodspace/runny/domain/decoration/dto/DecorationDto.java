package com.goodspace.runny.domain.decoration.dto;

import com.goodspace.runny.domain.decoration.entity.DecorationTemplate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 꾸미기 템플릿 요청/응답 DTO 모음. 저장 단위는 유저당 3칸(슬롯) 고정이다.
 */
public final class DecorationDto {

    private DecorationDto() {
    }

    /** 템플릿 저장 요청 - 사용자가 선택한 칸(slot 1~3)에 저장. name 미입력 시 "템플릿 {slot}" 자동 부여 */
    public record TemplateSaveRequest(
            @NotNull Integer slot,
            String name,
            @NotBlank String layoutJson
    ) {
    }

    /**
     * 슬롯 항목 - 빈 칸도 항상 내려간다(saved=false).
     * 프론트는 이 목록만으로 3칸 화면을 그대로 그릴 수 있다.
     */
    public record SlotItem(
            int slot,
            boolean saved,
            Long templateId,
            String name,
            String layoutJson,
            LocalDateTime createdAt
    ) {
        /** 저장된 칸 */
        public static SlotItem of(DecorationTemplate template) {
            return new SlotItem(template.getSlot(), true, template.getId(), template.getName(),
                    template.getLayoutJson(), template.getCreatedAt());
        }

        /** 빈 칸 */
        public static SlotItem empty(int slot) {
            return new SlotItem(slot, false, null, null, null, null);
        }
    }

    /** 템플릿 목록 응답 - 항상 slotCount만큼(3개) 반환한다 */
    public record TemplateListResponse(
            int slotCount,
            List<SlotItem> slots
    ) {
    }
}
