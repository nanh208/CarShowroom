package com.ares.controller;

import com.ares.dto.ApiResponse;
import com.ares.dto.PaymentDtos.CompletePaymentRequest;
import com.ares.dto.PaymentDtos.CreatePaymentRequest;
import com.ares.service.SandboxPaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/sandbox")
public class SandboxPaymentController {
    private final SandboxPaymentService payments;

    public SandboxPaymentController(SandboxPaymentService payments) {
        this.payments = payments;
    }

    @PostMapping("/orders/{orderCode}")
    public ResponseEntity<ApiResponse> create(@PathVariable String orderCode, @RequestBody CreatePaymentRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Đã tạo phiên thanh toán mô phỏng", payments.create(orderCode, request)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PostMapping("/transactions/{transactionRef}/complete")
    public ResponseEntity<ApiResponse> complete(@PathVariable String transactionRef, @RequestBody CompletePaymentRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Đã ghi nhận kết quả mô phỏng", payments.complete(transactionRef, request)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }
}
