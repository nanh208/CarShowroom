package com.ares.repository;

import com.ares.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByCodeAndActiveTrue(String code);
    List<Promotion> findByActiveTrue();
}
