package com.ares.warehouse.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 100)
    private String category;

    @Column(name = "reorder_point")
    private Integer reorderPoint = 0;

    protected InventoryItem() {}

    public InventoryItem(String sku, String name, String category) {
        this.sku = sku;
        this.name = name;
        this.category = category;
    }

    public InventoryItem(String sku, String name, String category, int reorderPoint) {
        this(sku, name, category);
        setReorderPoint(reorderPoint);
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public int getReorderPoint() { return reorderPoint == null ? 0 : reorderPoint; }
    public void setReorderPoint(int reorderPoint) {
        if (reorderPoint < 0) throw new IllegalArgumentException("Ngưỡng tồn tối thiểu không được âm.");
        this.reorderPoint = reorderPoint;
    }
}
