package com.ares.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuctionTests {
    @Test
    void startsCurrentPriceAtStartingPrice() {
        Auction auction = new Auction("Xe hiếm", "Toyota 2000GT", "/car.jpg", 100L, 10L,
                LocalDateTime.now(), LocalDateTime.now().plusHours(1));

        assertEquals(100L, auction.getCurrentPrice());
        assertEquals(AuctionStatus.UPCOMING, auction.getStatus());
    }
}
