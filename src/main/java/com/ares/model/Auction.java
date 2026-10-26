package com.ares.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

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

    private String image;

    @Column(nullable = false)
    private Long startingPrice;

    @Column(nullable = false)
    private Long currentPrice;

    @Column(nullable = false)
    private Long minimumIncrement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status = AuctionStatus.UPCOMING;

    @Column(nullable = false)
    private LocalDateTime startsAt;

    @Column(nullable = false)
    private LocalDateTime endsAt;

    public Auction() { }

    public Auction(String title, String vehicleName, String image, Long startingPrice,
                   Long minimumIncrement, LocalDateTime startsAt, LocalDateTime endsAt) {
        this.title = title;
        this.vehicleName = vehicleName;
        this.image = image;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.minimumIncrement = minimumIncrement;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Long getStartingPrice() { return startingPrice; }
    public void setStartingPrice(Long startingPrice) { this.startingPrice = startingPrice; }
    public Long getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(Long currentPrice) { this.currentPrice = currentPrice; }
    public Long getMinimumIncrement() { return minimumIncrement; }
    public void setMinimumIncrement(Long minimumIncrement) { this.minimumIncrement = minimumIncrement; }
    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }
}
