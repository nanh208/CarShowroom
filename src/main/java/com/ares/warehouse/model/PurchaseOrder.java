package com.ares.warehouse.model;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders", uniqueConstraints =
        @UniqueConstraint(columnNames = "order_code"))
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_code", nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 160)
    private String supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

    @Column(nullable = false)
    private long totalCost;

    @Column(length = 500)
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime orderedAt;
    private LocalDateTime receivedAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    protected PurchaseOrder() {}

    public PurchaseOrder(String code, String supplier, Warehouse warehouse, long totalCost, String note) {
        this.code = code;
        this.supplier = supplier;
        this.warehouse = warehouse;
        this.totalCost = totalCost;
        this.note = note;
    }

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    public void addLine(PurchaseOrderLine line) {
        lines.add(line);
        line.setPurchaseOrder(this);
    }

    public void markOrdered() {
        status = PurchaseOrderStatus.ORDERED;
        orderedAt = LocalDateTime.now();
    }

    public void markReceived() {
        status = PurchaseOrderStatus.RECEIVED;
        receivedAt = LocalDateTime.now();
    }

    public void cancel() { status = PurchaseOrderStatus.CANCELLED; }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getSupplier() { return supplier; }
    public Warehouse getWarehouse() { return warehouse; }
    public PurchaseOrderStatus getStatus() { return status; }
    public long getTotalCost() { return totalCost; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getOrderedAt() { return orderedAt; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public List<PurchaseOrderLine> getLines() { return lines; }
}
