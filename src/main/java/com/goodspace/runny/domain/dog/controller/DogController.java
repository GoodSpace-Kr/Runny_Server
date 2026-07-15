package com.goodspace.runny.domain.dog.controller;

import com.goodspace.runny.domain.dog.dto.DogDto;
import com.goodspace.runny.domain.dog.service.DogService;
import com.goodspace.runny.global.jwt.SecurityUtil;
import com.goodspace.runny.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 강아지 API 컨트롤러. 생성(온보딩/입양), 보유 목록, 활성 전환, 펫 프로필을 제공한다.
 */
@Tag(name = "Dog", description = "강아지 API - 생성/입양, 종 변경, 펫 프로필")
@RestController
@RequestMapping("/api/dogs")
@RequiredArgsConstructor
public class DogController {

    private final DogService dogService;

    /** 강아지 생성 (온보딩 첫 선택은 무료, 이후 입양은 코인 차감) */
    @Operation(summary = "강아지 생성 (온보딩/입양)",
            description = "온보딩(DOG_REQUIRED) 첫 생성: 일반 견종만 무료, 활성 지정 + 온보딩 COMPLETED. "
                    + "입양: 중복 보유 DOG_004, 미해금 레어 DOG_005, 잔액 부족 COIN_001 검증 후 코인 차감. "
                    + "이름 규칙: 1~7자 한글/영문/숫자(DOG_001), 비속어 불가(DOG_002), 중복 허용")
    @PostMapping
    public ApiResponse<DogDto.CreateResponse> createDog(@Valid @RequestBody DogDto.CreateRequest request) {
        return ApiResponse.ok(dogService.createDog(SecurityUtil.currentUserId(), request));
    }

    /** 보유 강아지 목록 (종 변경 화면용) */
    @Operation(summary = "보유 강아지 목록", description = "설정한 이름으로 표시. active 필드로 현재 활성 강아지 구분")
    @GetMapping("/me")
    public ApiResponse<List<DogDto.MyDogResponse>> getMyDogs() {
        return ApiResponse.ok(dogService.getMyDogs(SecurityUtil.currentUserId()));
    }

    /** 활성 강아지 전환 (종 변경) */
    @Operation(summary = "활성 강아지 전환",
            description = "착용 코디는 강아지별로 저장되어 유지된다. 미보유 강아지 지정 시 DOG_007")
    @PatchMapping("/active")
    public ApiResponse<Void> changeActiveDog(@Valid @RequestBody DogDto.ActiveRequest request) {
        dogService.changeActiveDog(SecurityUtil.currentUserId(), request.dogId());
        return ApiResponse.ok();
    }

    /** 펫 프로필 조회 (미확인 변화 요약 포함, 조회 시 확인 처리) */
    @Operation(summary = "펫 프로필 조회",
            description = "이름/종/견종 소개/레벨/성장 단계/경험치(다음 레벨까지)/스탯 + 미확인 변화 요약(changeSummary). "
                    + "응답과 동시에 미확인 로그가 일괄 확인(seen) 처리되어 빨간 점이 해제된다. 활성 강아지 없으면 DOG_009")
    @GetMapping("/active/profile")
    public ApiResponse<DogDto.ProfileResponse> getActiveProfile() {
        return ApiResponse.ok(dogService.getActiveProfile(SecurityUtil.currentUserId()));
    }
}
