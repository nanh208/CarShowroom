package com.ares.warehouse.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse_stock_movements")
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private InventoryItem item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_warehouse_id")
    private Warehouse fromWarehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_warehouse_id")
    private Warehouse toWarehouse;

    @Column(nullable = false, length = 20)
    private String movementType;

    @Column(nullable = false)
    private int quantity;

    @Column(length = 300)
    private String note;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected StockMovement() {}

    public StockMovement(InventoryItem item, Warehouse fromWarehouse, Warehouse toWarehouse,
                         String movementType, int quantity, String note) {
        this.item = item;
        this.fromWarehouse = fromWarehouse;
        this.toWarehouse = toWarehouse;
        this.movementType = movementType;
        this.quantity = quantity;
        this.note = note;
    }

    public Long getId() { return id; }
    public InventoryItem getItem() { return item; }
    public Warehouse getFromWarehouse() { return fromWarehouse; }
    public Warehouse getToWarehouse() { return toWarehouse; }
    public String getMovementType() { return movementType; }
    public int getQuantity() { return quantity; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
