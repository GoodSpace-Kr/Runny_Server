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

import java.util.List;

/**
 * 꾸미기 템플릿 API 컨트롤러. 리포트 꾸미기의 위젯 배치를 저장해 다음 러닝에서 재사용할 수 있게 한다.
 */
@Tag(name = "Decoration", description = "꾸미기 템플릿 API - 위젯 배치 레이아웃 저장/조회/삭제")
@RestController
@RequestMapping("/api/decorations/templates")
@RequiredArgsConstructor
public class DecorationController {

    private final DecorationTemplateService decorationTemplateService;

    /** 내 템플릿 목록 */
    @Operation(summary = "꾸미기 템플릿 목록",
            description = "최신순. layoutJson은 프론트 스키마이며 서버는 해석하지 않는다. "
                    + "배경 사진은 저장되지 않고 지표 값은 적용 시점의 러닝 기록으로 채운다")
    @GetMapping
    public ApiResponse<List<DecorationDto.TemplateItem>> getTemplates() {
        return ApiResponse.ok(decorationTemplateService.getTemplates(SecurityUtil.currentUserId()));
    }

    /** 템플릿 저장 */
    @Operation(summary = "꾸미기 템플릿 저장",
            description = "위젯 배치 레이아웃(JSON) 저장. 유저당 최대 10개(DECORATION_001), "
                    + "20KB 초과 DECORATION_002, JSON 형식 오류 DECORATION_003. name 미입력 시 자동 부여")
    @PostMapping
    public ApiResponse<DecorationDto.TemplateItem> save(
            @Valid @RequestBody DecorationDto.TemplateSaveRequest request) {
        return ApiResponse.ok(decorationTemplateService.save(SecurityUtil.currentUserId(), request));
    }

    /** 템플릿 삭제 */
    @Operation(summary = "꾸미기 템플릿 삭제", description = "본인 소유 템플릿만 삭제 가능(없으면 DECORATION_005)")
    @DeleteMapping("/{templateId}")
    public ApiResponse<Void> delete(@PathVariable Long templateId) {
        decorationTemplateService.delete(SecurityUtil.currentUserId(), templateId);
        return ApiResponse.ok();
    }
}
