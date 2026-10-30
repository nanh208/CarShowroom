package com.ares.repository;

import com.ares.model.Auction;
import com.ares.model.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    List<Auction> findByStatus(AuctionStatus status);
    List<Auction> findByStatusOrderByEndsAtAsc(AuctionStatus status);
}
