package com.ares.warehouse.repository;

import com.ares.warehouse.model.StockMovement;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findTop100ByOrderByCreatedAtDesc();
    @EntityGraph(attributePaths = {"item", "fromWarehouse", "toWarehouse"})
    List<StockMovement> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime until);
}
