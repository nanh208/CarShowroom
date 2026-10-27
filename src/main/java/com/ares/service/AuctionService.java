package com.ares.service;

import com.ares.model.Auction;
import com.ares.model.AuctionStatus;
import com.ares.repository.AuctionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuctionService {
    private final AuctionRepository auctionRepository;

    public AuctionService(AuctionRepository auctionRepository) {
        this.auctionRepository = auctionRepository;
    }

    public List<Auction> findAll(AuctionStatus status) {
        refreshTimedStatuses();
        return status == null ? auctionRepository.findAll() : auctionRepository.findByStatusOrderByEndsAtAsc(status);
    }

    public Optional<Auction> findById(Long id) {
        refreshTimedStatuses();
        return auctionRepository.findById(id);
    }

    private void refreshTimedStatuses() {
        LocalDateTime now = LocalDateTime.now();
        for (Auction auction : auctionRepository.findAll()) {
            AuctionStatus expected = now.isBefore(auction.getStartsAt()) ? AuctionStatus.UPCOMING
                    : now.isAfter(auction.getEndsAt()) ? AuctionStatus.ENDED : AuctionStatus.LIVE;
            if (auction.getStatus() != AuctionStatus.CANCELLED && auction.getStatus() != expected) {
                auction.setStatus(expected);
                auctionRepository.save(auction);
            }
        }
    }
}
