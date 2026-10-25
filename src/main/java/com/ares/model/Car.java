package com.ares.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cars")
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String code;

    @Column(nullable = false)
    private Long price;

    private Long oldPrice;

    private String formattedPrice;

    private String formattedOldPrice;

    private Integer discountPercent = 20;

    @Column(nullable = false)
    private String image;

    private String section; // BEST_SELLER, RC_HIGHLIGHTS, MODEL_HIGHLIGHTS

    private String category; // RC_CARS, MODEL_CARS, BATTERIES, CONTROLLERS, OTHER_PARTS

    private String scale; // 1:18, 1:10

    private String material;

    private String features;

    @Column(length = 1000)
    private String description;

    private Double rating = 5.0;

    private Integer reviewCount = 128;

    private Integer soldCount = 340;

    private Boolean inStock = true;

    public Car() {}

    public Car(String name, String code, Long price, Long oldPrice, String image, 
               String section, String category, String scale, String material, 
               String features, String description) {
        this.name = name;
        this.code = code;
        this.price = price;
        this.oldPrice = oldPrice;
        this.formattedPrice = formatVnd(price);
        this.formattedOldPrice = oldPrice != null ? formatVnd(oldPrice) : null;
        if (oldPrice != null && oldPrice > price) {
            this.discountPercent = (int) Math.round((1.0 - (double) price / oldPrice) * 100);
        }
        this.image = image;
        this.section = section;
        this.category = category;
        this.scale = scale;
        this.material = material;
        this.features = features;
        this.description = description;
    }

    private static String formatVnd(Long amount) {
        if (amount == null) return "Liên hệ";
        return String.format("%,d₫", amount).replace(',', '.');
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Long getPrice() { return price; }
    public void setPrice(Long price) {
        this.price = price;
        this.formattedPrice = formatVnd(price);
    }

    public Long getOldPrice() { return oldPrice; }
    public void setOldPrice(Long oldPrice) {
        this.oldPrice = oldPrice;
        this.formattedOldPrice = oldPrice != null ? formatVnd(oldPrice) : null;
    }

    public String getFormattedPrice() { return formattedPrice != null ? formattedPrice : formatVnd(price); }
    public void setFormattedPrice(String formattedPrice) { this.formattedPrice = formattedPrice; }

    public String getFormattedOldPrice() { return formattedOldPrice != null ? formattedOldPrice : (oldPrice != null ? formatVnd(oldPrice) : null); }
    public void setFormattedOldPrice(String formattedOldPrice) { this.formattedOldPrice = formattedOldPrice; }

    public Integer getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(Integer discountPercent) { this.discountPercent = discountPercent; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getScale() { return scale; }
    public void setScale(String scale) { this.scale = scale; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public String getFeatures() { return features; }
    public void setFeatures(String features) { this.features = features; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

    public Integer getSoldCount() { return soldCount; }
    public void setSoldCount(Integer soldCount) { this.soldCount = soldCount; }

    public Boolean getInStock() { return inStock; }
    public void setInStock(Boolean inStock) { this.inStock = inStock; }
}
