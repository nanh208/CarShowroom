package com.ares.repository;

import com.ares.model.Car;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CarRepository extends JpaRepository<Car, Long> {
    List<Car> findBySection(String section);
    List<Car> findByCategory(String category);
    List<Car> findByNameContainingIgnoreCase(String keyword);

    @Query("""
            select c from Car c
            where (:section is null or upper(c.section) = :section)
              and (:category is null or upper(c.category) = :category)
              and (:search is null or lower(c.name) like lower(concat('%', :search, '%'))
                   or lower(coalesce(c.code, '')) like lower(concat('%', :search, '%')))
              and (:minPrice is null or c.price >= :minPrice)
              and (:maxPrice is null or c.price <= :maxPrice)
              and (:inStock is null or c.inStock = :inStock)
            """)
    List<Car> findByFilters(
            @Param("section") String section,
            @Param("category") String category,
            @Param("search") String search,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            @Param("inStock") Boolean inStock);
}
