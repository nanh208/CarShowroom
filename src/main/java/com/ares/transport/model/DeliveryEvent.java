package com.ares.transport.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "delivery_events")
public class DeliveryEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private DeliveryStatus status;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false)
    private LocalDateTime occurredAt;

    protected DeliveryEvent() {}

    public DeliveryEvent(Delivery delivery, DeliveryStatus status, String description) {
        this.delivery = delivery;
        this.status = status;
        this.description = description;
    }

    @PrePersist
    void onCreate() { occurredAt = LocalDateTime.now(); }

    public DeliveryStatus getStatus() { return status; }
    public String getDescription() { return description; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
