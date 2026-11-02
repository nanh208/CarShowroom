package com.ares.warehouse.repository;

import com.ares.warehouse.model.Warehouse;
import com.ares.warehouse.model.WarehouseStock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseStockRepository extends JpaRepository<WarehouseStock, Long> {
    List<WarehouseStock> findAllByOrderByItem_NameAsc();
    List<WarehouseStock> findByWarehouse(Warehouse warehouse);
    Optional<WarehouseStock> findByWarehouseIdAndItemId(Long warehouseId, Long itemId);
}
