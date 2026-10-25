package com.ares.transport.repository;

import com.ares.transport.model.Delivery;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findAllByOrderByCreatedAtDesc();
    boolean existsByTrackingNumber(String trackingNumber);
}
