package com.goodspace.runny.domain.dog.repository;

import com.goodspace.runny.domain.dog.entity.DogBreed;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 견종 마스터 리포지토리.
 */
public interface DogBreedRepository extends JpaRepository<DogBreed, Long> {

    List<DogBreed> findAllByOrderByIdAsc();
}
