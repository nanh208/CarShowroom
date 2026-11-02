package com.ares.repository;

import com.ares.model.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    List<ReturnRequest> findByOrder_IdOrderByCreatedAtDesc(Long orderId);
    Optional<ReturnRequest> findByRequestCode(String requestCode);
    boolean existsByOrder_IdAndStatusIn(Long orderId, Collection<String> statuses);
}
