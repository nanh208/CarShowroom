package com.ares.transport.model;

public enum DeliveryStatus {
    READY("Chờ bàn giao"),
    IN_TRANSIT("Đang vận chuyển"),
    OUT_FOR_DELIVERY("Đang giao"),
    DELIVERED("Đã giao"),
    RETURNED("Hoàn hàng");

    private final String label;

    DeliveryStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
