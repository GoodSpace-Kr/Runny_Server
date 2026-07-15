package com.goodspace.runny.domain.dog.entity;

import com.goodspace.runny.domain.item.entity.ItemCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 강아지별 착용 코디 엔티티. 아이템 보유는 계정 단위지만 착용 상태는 강아지 단위로 저장된다.
 * (강아지+카테고리) UNIQUE로 슬롯당 1개 착용을 보장한다. 실제 착용 로직은 4단계 드레스룸에서 사용.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "dog_equipment", uniqueConstraints = {
        @UniqueConstraint(name = "uk_dog_equipment_slot", columnNames = {"user_dog_id", "category"})
})
public class DogEquipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_dog_id", nullable = false)
    private Long userDogId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ItemCategory category;

    // 아이템 마스터 FK (item 엔티티는 4단계 구현 - 지금은 ID 컬럼만 보유)
    @Column(name = "item_id", nullable = false)
    private Long itemId;

    public DogEquipment(Long userDogId, ItemCategory category, Long itemId) {
        this.userDogId = userDogId;
        this.category = category;
        this.itemId = itemId;
    }

    /** 슬롯의 착용 아이템 교체 */
    public void changeItem(Long itemId) {
        this.itemId = itemId;
    }
}
