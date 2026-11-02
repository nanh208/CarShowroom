package com.ares.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "auctions")
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String vehicleName;

    private String brandName;

    @Column(name = "model_year")
    private Integer modelYear;

    private String mileage;

    @Column(length = 3000)
    private String description;

    private String image;

    @ElementCollection(fetch = FetchType.EAGER)
    @OrderColumn
    private List<String> images = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "spec_key")
    @Column(name = "spec_value")
    private Map<String, String> specs = new LinkedHashMap<>();

    @Column(nullable = false)
    private Long startingPrice;

    @Column(nullable = false)
    private Long currentPrice;

    @Column(nullable = false)
    private Long minimumIncrement;

    private Integer bidCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status = AuctionStatus.UPCOMING;

    @Column(nullable = false)
    private LocalDateTime startsAt;

    @Column(nullable = false)
    private LocalDateTime endsAt;

    private Boolean cancelled = false;

    private Boolean finalized = false;

    @JsonIgnoreProperties({"password", "phone", "email"})
    @ManyToOne(fetch = FetchType.EAGER)
    private User winner;

    private String orderCode;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Auction() { }

    public Auction(String title, String vehicleName, String brandName, Integer modelYear,
                   String mileage, String description, String image, Long startingPrice,
                   Long minimumIncrement, LocalDateTime startsAt, LocalDateTime endsAt) {
        this.title = title;
        this.vehicleName = vehicleName;
        this.brandName = brandName;
        this.modelYear = modelYear;
        this.mileage = mileage;
        this.description = description;
        this.image = image;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.minimumIncrement = minimumIncrement;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public Auction(String title, String vehicleName, String image, Long startingPrice,
                   Long minimumIncrement, LocalDateTime startsAt, LocalDateTime endsAt) {
        this(title, vehicleName, null, null, null, null, image, startingPrice,
                minimumIncrement, startsAt, endsAt);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    public Integer getModelYear() { return modelYear; }
    public void setModelYear(Integer modelYear) { this.modelYear = modelYear; }
    public String getMileage() { return mileage; }
    public void setMileage(String mileage) { this.mileage = mileage; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public Map<String, String> getSpecs() { return specs; }
    public void setSpecs(Map<String, String> specs) { this.specs = specs; }
    public Long getStartingPrice() { return startingPrice; }
    public void setStartingPrice(Long startingPrice) { this.startingPrice = startingPrice; }
    public Long getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(Long currentPrice) { this.currentPrice = currentPrice; }
    public Long getMinimumIncrement() { return minimumIncrement; }
    public void setMinimumIncrement(Long minimumIncrement) { this.minimumIncrement = minimumIncrement; }
    public Integer getBidCount() { return bidCount; }
    public void setBidCount(Integer bidCount) { this.bidCount = bidCount; }
    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }
    public Boolean getCancelled() { return cancelled; }
    public void setCancelled(Boolean cancelled) { this.cancelled = cancelled; }
    public Boolean getFinalized() { return finalized; }
    public void setFinalized(Boolean finalized) { this.finalized = finalized; }
    public User getWinner() { return winner; }
    public void setWinner(User winner) { this.winner = winner; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public long getNextMinBid() {
        return (bidCount == null || bidCount == 0) ? startingPrice : currentPrice + minimumIncrement;
    }
}
