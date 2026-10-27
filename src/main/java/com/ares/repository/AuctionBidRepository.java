package com.ares.repository;

import com.ares.model.AuctionBid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuctionBidRepository extends JpaRepository<AuctionBid, Long> {
    List<AuctionBid> findByAuctionIdOrderByPlacedAtDesc(Long auctionId);
}
