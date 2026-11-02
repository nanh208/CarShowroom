package com.ares.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "promotions")
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private String title;

    private String discountType = "PERCENTAGE"; // PERCENTAGE hoáº·c FIXED_AMOUNT

    private Long discountValue = 0L; // Sá»‘ % hoáº·c sá»‘ tiá»n VNÄ

    private Long minOrderValue = 0L;

    private Long maxDiscount = 0L;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private Integer usageLimit = 100;

    private Integer usedCount = 0;

    private Boolean active = true;

    public Promotion() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }
    public Long getDiscountValue() { return discountValue; }
    public void setDiscountValue(Long discountValue) { this.discountValue = discountValue; }
    public Long getMinOrderValue() { return minOrderValue; }
    public void setMinOrderValue(Long minOrderValue) { this.minOrderValue = minOrderValue; }
    public Long getMaxDiscount() { return maxDiscount; }
    public void setMaxDiscount(Long maxDiscount) { this.maxDiscount = maxDiscount; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    public LocalDateTime getValidTo() { return validTo; }
    public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }
    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }
    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
