package com.ares.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "customer_order_items")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @Column(nullable = false)
    private Long carId;

    @Column(nullable = false, length = 80)
    private String productCode;

    @Column(nullable = false, length = 160)
    private String carName;

    @Column(length = 500)
    private String image;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private long unitPrice;

    @Column(nullable = false)
    private long subtotal;

    public OrderLine() {}

    public OrderLine(Long carId, String productCode, String carName, String image, int quantity, long unitPrice, long subtotal) {
        this.carId = carId;
        this.productCode = productCode;
        this.carName = carName;
        this.image = image;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    public Long getId() { return id; }
    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }
    public Long getCarId() { return carId; }
    public String getProductCode() { return productCode; }
    public String getCarName() { return carName; }
    public String getImage() { return image; }
    public int getQuantity() { return quantity; }
    public long getUnitPrice() { return unitPrice; }
    public long getSubtotal() { return subtotal; }
}
