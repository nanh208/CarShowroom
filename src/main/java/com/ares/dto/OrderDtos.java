package com.ares.dto;

import java.time.LocalDateTime;
import java.util.List;

public class OrderDtos {

    public record OrderLineRequest(String productCode, Integer quantity) {}

    public record CreateOrderRequest(
            String customerName,
            String phone,
            String email,
            String shippingAddress,
            String note,
            List<OrderLineRequest> items) {}

    public record OrderLineResponse(
            String productCode,
            Long carId,
            String carName,
            String image,
            int quantity,
            long unitPrice,
            long subtotal) {}

    public record CreateOrderResponse(
            String code,
            String status,
            String paymentMethod,
            long total,
            int totalQuantity,
            LocalDateTime createdAt,
            List<OrderLineResponse> items) {}
}
