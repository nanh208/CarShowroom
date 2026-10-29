package com.ares.transport.repository;

import com.ares.transport.model.DeliveryEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeliveryEventRepository extends JpaRepository<DeliveryEvent, Long> {
    List<DeliveryEvent> findAllByDeliveryIdOrderByOccurredAtAsc(Long deliveryId);
}
