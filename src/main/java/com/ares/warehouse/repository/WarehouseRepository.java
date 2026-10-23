package com.ares.warehouse.repository;

import com.ares.warehouse.model.Warehouse;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    List<Warehouse> findAllByOrderByNameAsc();
    Optional<Warehouse> findByCode(String code);
}
