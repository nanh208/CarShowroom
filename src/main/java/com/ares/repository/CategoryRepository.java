package com.ares.repository;

import com.ares.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByCode(String code);
    List<Category> findAllByOrderBySortOrderAscCodeAsc();
}
