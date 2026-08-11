package com.goodspace.runny.domain.decoration.service;

import com.goodspace.runny.domain.decoration.dto.DecorationDto;
import com.goodspace.runny.domain.decoration.entity.DecorationTemplate;
import com.goodspace.runny.domain.decoration.repository.DecorationTemplateRepository;
import com.goodspace.runny.global.exception.BusinessException;
import com.goodspace.runny.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 꾸미기 템플릿 서비스. 유저당 3칸(슬롯) 고정으로 위젯 배치 레이아웃(JSON)만 저장/조회/삭제한다.
 * 이미 저장된 칸에는 덮어쓰지 않고 삭제 후 저장하도록 하며(사용자가 실수로 기존 템플릿을 잃지 않게),
 * 서버는 layoutJson의 내용을 해석하지 않고 파싱 가능 여부와 크기만 검증한다.
 */
@Service
@RequiredArgsConstructor
public class DecorationTemplateService {

    // 유저당 템플릿 저장 칸 수 (템플릿 1~3)
    public static final int SLOT_COUNT = 3;
    // layoutJson 최대 크기 (바이트)
    private static final int MAX_LAYOUT_BYTES = 20 * 1024;
    private static final int MAX_NAME_LENGTH = 20;

    private final DecorationTemplateRepository decorationTemplateRepository;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    /** 내 템플릿 목록 - 빈 칸을 포함해 항상 3칸을 반환한다 */
    @Transactional(readOnly = true)
    public DecorationDto.TemplateListResponse getTemplates(Long userId) {
        List<DecorationTemplate> saved = decorationTemplateRepository.findByUserIdOrderBySlotAsc(userId);
        List<DecorationDto.SlotItem> slots = IntStream.rangeClosed(1, SLOT_COUNT)
                .mapToObj(slot -> saved.stream()
                        .filter(template -> template.getSlot() == slot)
                        .findFirst()
                        .map(DecorationDto.SlotItem::of)
                        .orElseGet(() -> DecorationDto.SlotItem.empty(slot)))
                .toList();
        return new DecorationDto.TemplateListResponse(SLOT_COUNT, slots);
    }

    /**
     * 템플릿 저장 - 사용자가 선택한 빈 칸에 저장한다.
     * 이미 저장된 칸이면 DECORATION_006으로 거절하며, 3칸이 모두 찼다면 하나를 삭제한 뒤 저장해야 한다.
     */
    @Transactional
    public DecorationDto.SlotItem save(Long userId, DecorationDto.TemplateSaveRequest request) {
        int slot = request.slot();
        validateSlot(slot);
        validateLayout(request.layoutJson());

        // 이미 사용 중인 칸이면 덮어쓰지 않는다 (삭제 후 저장 정책)
        if (decorationTemplateRepository.findByUserIdAndSlot(userId, slot).isPresent()) {
            throw new BusinessException(ErrorCode.DECORATION_006);
        }

        String name = (request.name() == null || request.name().isBlank())
                ? "템플릿 " + slot
                : request.name().trim();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.DECORATION_004);
        }
        try {
            DecorationTemplate saved = decorationTemplateRepository
                    .saveAndFlush(new DecorationTemplate(userId, slot, name, request.layoutJson()));
            return DecorationDto.SlotItem.of(saved);
        } catch (DataIntegrityViolationException e) {
            // 같은 칸에 동시 저장 경쟁 - (user_id, slot) UNIQUE 위반을 비즈니스 예외로 변환
            throw new BusinessException(ErrorCode.DECORATION_006);
        }
    }

    /** 템플릿 삭제 - 해당 칸을 비운다. 본인 소유만 가능하며 빈 칸이면 DECORATION_005 */
    @Transactional
    public void delete(Long userId, int slot) {
        validateSlot(slot);
        DecorationTemplate template = decorationTemplateRepository.findByUserIdAndSlot(userId, slot)
                .orElseThrow(() -> new BusinessException(ErrorCode.DECORATION_005));
        decorationTemplateRepository.delete(template);
    }

    /** 회원 탈퇴 훅 - 저장한 템플릿 전부 삭제 */
    @Transactional
    public void deleteAllOf(Long userId) {
        decorationTemplateRepository.deleteAll(decorationTemplateRepository.findByUserIdOrderBySlotAsc(userId));
    }

    /** 슬롯 번호 검증 - 1~3만 허용 */
    private void validateSlot(int slot) {
        if (slot < 1 || slot > SLOT_COUNT) {
            throw new BusinessException(ErrorCode.DECORATION_001);
        }
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
