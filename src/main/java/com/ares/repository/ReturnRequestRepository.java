package com.ares.repository;

import com.ares.model.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    List<ReturnRequest> findAllByOrderByCreatedAtDesc();
    List<ReturnRequest> findByOrder_IdOrderByCreatedAtDesc(Long orderId);
    Optional<ReturnRequest> findByRequestCode(String requestCode);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ReturnRequest r where r.requestCode = :code")
    Optional<ReturnRequest> findByRequestCodeForUpdate(@Param("code") String code);
    boolean existsByOrder_IdAndStatusIn(Long orderId, Collection<String> statuses);
}
