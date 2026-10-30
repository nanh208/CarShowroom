package com.ares.warehouse.repository;

import com.ares.warehouse.model.InventoryItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    List<InventoryItem> findAllByOrderByNameAsc();
    Optional<InventoryItem> findBySkuIgnoreCase(String sku);
}
