package com.ares.repository;

import com.ares.model.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
    List<Car> findAllByOrderByCategoryAscNameAsc();
    List<Car> findBySection(String section);
    List<Car> findByCategory(String category);
    List<Car> findByNameContainingIgnoreCase(String keyword);
    List<Car> findAllByCodeIgnoreCase(String code);
}
