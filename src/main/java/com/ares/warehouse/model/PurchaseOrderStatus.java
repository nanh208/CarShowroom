package com.ares.warehouse.model;

public enum PurchaseOrderStatus {
    DRAFT("Bản nháp"),
    ORDERED("Đã đặt hàng"),
    RECEIVED("Đã nhận đủ"),
    CANCELLED("Đã hủy");

    private final String label;

    PurchaseOrderStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
