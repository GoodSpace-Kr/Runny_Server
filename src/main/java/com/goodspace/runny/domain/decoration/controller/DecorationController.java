package com.goodspace.runny.domain.decoration.controller;

import com.goodspace.runny.domain.decoration.dto.DecorationDto;
import com.goodspace.runny.domain.decoration.service.DecorationTemplateService;
import com.goodspace.runny.global.jwt.SecurityUtil;
import com.goodspace.runny.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 꾸미기 템플릿 API 컨트롤러. 리포트 꾸미기의 위젯 배치를 3칸(슬롯) 중 원하는 칸에 저장해 재사용할 수 있게 한다.
 */
@Tag(name = "Decoration", description = "꾸미기 템플릿 API - 3칸 슬롯 기반 위젯 배치 저장/조회/삭제")
@RestController
@RequestMapping("/api/decorations/templates")
@RequiredArgsConstructor
public class DecorationController {

    private final DecorationTemplateService decorationTemplateService;

    /** 내 템플릿 목록 (3칸 고정) */
    @Operation(summary = "꾸미기 템플릿 목록",
            description = "빈 칸을 포함해 항상 3칸을 반환한다(saved=false면 빈 칸). "
                    + "layoutJson은 프론트 스키마이며 서버는 해석하지 않는다. "
                    + "배경 사진은 저장되지 않고 지표 값은 적용 시점의 러닝 기록으로 채운다")
    @GetMapping
    public ApiResponse<DecorationDto.TemplateListResponse> getTemplates() {
        return ApiResponse.ok(decorationTemplateService.getTemplates(SecurityUtil.currentUserId()));
    }

    /** 템플릿 저장 (칸 선택) */
    @Operation(summary = "꾸미기 템플릿 저장",
            description = "사용자가 선택한 빈 칸(slot 1~3)에 저장한다. "
                    + "이미 저장된 칸이면 DECORATION_006(삭제 후 저장), 슬롯 범위 밖이면 DECORATION_001, "
                    + "20KB 초과 DECORATION_002, JSON 형식 오류 DECORATION_003. name 미입력 시 \"템플릿 {slot}\" 자동 부여")
    @PostMapping
    public ApiResponse<DecorationDto.SlotItem> save(
            @Valid @RequestBody DecorationDto.TemplateSaveRequest request) {
        return ApiResponse.ok(decorationTemplateService.save(SecurityUtil.currentUserId(), request));
    }

    /** 템플릿 삭제 (칸 비우기) */
    @Operation(summary = "꾸미기 템플릿 삭제",
            description = "해당 칸을 비운다. 빈 칸이면 DECORATION_005. 3칸이 모두 찬 상태에서 새로 저장하려면 먼저 호출한다")
    @DeleteMapping("/{slot}")
    public ApiResponse<Void> delete(@PathVariable int slot) {
        decorationTemplateService.delete(SecurityUtil.currentUserId(), slot);
        return ApiResponse.ok();
    }
}
