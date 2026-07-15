package com.goodspace.runny.domain.dog.repository;

import com.goodspace.runny.domain.dog.entity.UserDog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 보유 강아지 리포지토리.
 */
public interface UserDogRepository extends JpaRepository<UserDog, Long> {

    List<UserDog> findByUserIdOrderByIdAsc(Long userId);

    Optional<UserDog> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndBreedId(Long userId, Long breedId);

    /** 유저가 보유한 견종 ID 목록 (견종 목록의 보유 여부 표시용) */
    List<UserDog> findByUserId(Long userId);
}
