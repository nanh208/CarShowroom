package com.ares.transport.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "deliveries")
public class Delivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String orderCode;

    @Column(nullable = false, length = 160)
    private String recipientName;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 20)
    private String carrier;

    @Column(nullable = false, unique = true, length = 40)
    private String trackingNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private DeliveryStatus status = DeliveryStatus.READY;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Delivery() {}

    public Delivery(String orderCode, String recipientName, String phone, String address, String city,
                    String carrier, String trackingNumber) {
        this.orderCode = orderCode;
        this.recipientName = recipientName;
        this.phone = phone;
        this.address = address;
        this.city = city;
        this.carrier = carrier;
        this.trackingNumber = trackingNumber;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    public void markUpdated() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getOrderCode() { return orderCode; }
    public String getRecipientName() { return recipientName; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getCarrier() { return carrier; }
    public String getTrackingNumber() { return trackingNumber; }
    public DeliveryStatus getStatus() { return status; }
    public void setStatus(DeliveryStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
