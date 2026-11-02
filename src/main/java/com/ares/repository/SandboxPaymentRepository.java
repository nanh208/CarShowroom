package com.ares.repository;

import com.ares.model.SandboxPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SandboxPaymentRepository extends JpaRepository<SandboxPayment, Long> {
    Optional<SandboxPayment> findByTransactionRef(String transactionRef);
    List<SandboxPayment> findByOrderCodeAndStatus(String orderCode, String status);
}
