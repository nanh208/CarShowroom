package com.ares.dto;

import java.time.LocalDateTime;

public class PaymentDtos {
    public record CreatePaymentRequest(String orderToken, String provider) {}
    public record CompletePaymentRequest(String orderToken, String responseCode) {}
    public record PaymentResponse(String transactionRef, String orderCode, String provider, long amount,
                                  String status, LocalDateTime expiresAt, String paymentStatus) {}
}
