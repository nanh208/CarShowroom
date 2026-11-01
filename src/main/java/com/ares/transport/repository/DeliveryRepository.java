package com.ares.transport.repository;

import com.ares.transport.model.Delivery;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findAllByOrderByCreatedAtDesc();
    boolean existsByTrackingNumber(String trackingNumber);
    Optional<Delivery> findByTrackingNumberIgnoreCase(String trackingNumber);
    boolean existsByOrderId(Long orderId);

    @Query("select d.orderId from Delivery d where d.orderId is not null")
    List<Long> findLinkedOrderIds();
}
