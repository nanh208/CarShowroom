package com.ares.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ReturnDtos {
    public record ReturnLineRequest(String productCode, Integer quantity) {}
    public record ReturnOrderRequest(String phone) {}
    public record CreateReturnRequest(String phone, String kind, String reason, List<ReturnLineRequest> items) {}
    public record ReturnableItem(String productCode, String productName, int purchasedQuantity, int alreadyRequestedQuantity,
                                 int availableQuantity, long unitPrice) {}
    public record ReturnableOrder(String orderCode, LocalDateTime createdAt, LocalDateTime returnDeadline,
                                  List<ReturnableItem> items, boolean hasOpenRequest) {}
    public record ReturnResponse(String requestCode, String orderCode, String kind, String status,
                                 long estimatedRefundAmount, LocalDateTime createdAt) {}
}
