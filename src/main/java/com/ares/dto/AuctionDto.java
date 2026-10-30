package com.ares.dto;

import com.ares.model.Auction;
import com.ares.model.AuctionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AuctionDto(
    Long id,
    String title,
    String vehicleName,
    String brandName,
    Integer modelYear,
    String mileage,
    String description,
    String image,
    List<String> images,
    Map<String, String> specs,
    Long startingPrice,
    Long currentPrice,
    Long minimumIncrement,
    Long nextMinBid,
    Integer bidCount,
    AuctionStatus status,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    Boolean finalized,
    String winnerName,
    String orderCode
) {
    public static AuctionDto from(Auction a) {
        String winner = a.getWinner() != null ? a.getWinner().getUsername() : null;
        return new AuctionDto(
            a.getId(), a.getTitle(), a.getVehicleName(), a.getBrandName(),
            a.getModelYear(), a.getMileage(), a.getDescription(), a.getImage(),
            a.getImages(), a.getSpecs(), a.getStartingPrice(), a.getCurrentPrice(),
            a.getMinimumIncrement(), a.getNextMinBid(), a.getBidCount(),
            a.getStatus(), a.getStartsAt(), a.getEndsAt(), a.getFinalized(),
            winner, a.getOrderCode()
        );
    }
}
