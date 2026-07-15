package com.goodspace.runny.domain.dog.controller;

import com.goodspace.runny.domain.dog.dto.DogDto;
import com.goodspace.runny.domain.dog.service.DogService;
import com.goodspace.runny.global.jwt.SecurityUtil;
import com.goodspace.runny.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 견종 API 컨트롤러. 온보딩 종 선택과 입양 화면이 공용으로 사용한다.
 */
@Tag(name = "Breed", description = "견종 마스터 API - 온보딩/입양 화면 공용")
@RestController
@RequestMapping("/api/breeds")
@RequiredArgsConstructor
public class BreedController {

    private final DogService dogService;

    /** 견종 목록 조회 */
    @Operation(summary = "견종 목록 조회",
            description = "등급/가격/소개 문구/보유 여부(owned)/업적 해금형 여부(achievementLocked)/해금 여부(unlocked) 포함. "
                    + "온보딩에서는 NORMAL만 선택 가능하며 레어는 노출만 된다(선택 시 DOG_006)")
    @GetMapping
    public ApiResponse<List<DogDto.BreedResponse>> getBreeds() {
        return ApiResponse.ok(dogService.getBreeds(SecurityUtil.currentUserId()));
    }
}
