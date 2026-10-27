package com.ares.service;

import com.ares.model.Auction;
import com.ares.model.AuctionBid;
import com.ares.model.AuctionStatus;
import com.ares.model.User;
import com.ares.repository.AuctionBidRepository;
import com.ares.repository.AuctionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AuctionService {
    private final AuctionRepository auctionRepository;
    private final AuctionBidRepository auctionBidRepository;

    public AuctionService(AuctionRepository auctionRepository, AuctionBidRepository auctionBidRepository) {
        this.auctionRepository = auctionRepository;
        this.auctionBidRepository = auctionBidRepository;
    }

    public List<Auction> findAll(AuctionStatus status) {
        refreshTimedStatuses();
        return status == null ? auctionRepository.findAll() : auctionRepository.findByStatusOrderByEndsAtAsc(status);
    }

    public Optional<Auction> findById(Long id) {
        refreshTimedStatuses();
        return auctionRepository.findById(id);
    }

    @Transactional
    public synchronized AuctionBid placeBid(Long auctionId, User bidder, Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Giá đặt phải lớn hơn 0");
        }

        refreshTimedStatuses();
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên đấu giá"));
        if (auction.getStatus() != AuctionStatus.LIVE) {
            throw new IllegalArgumentException("Phiên đấu giá hiện không diễn ra");
        }

        long nextMinimum = auction.getCurrentPrice() + auction.getMinimumIncrement();
        if (amount < nextMinimum) {
            throw new IllegalArgumentException("Giá đặt tối thiểu là " + nextMinimum);
        }

        auctionBidRepository.findFirstByAuctionIdOrderByAmountDesc(auctionId).ifPresent(highest -> {
            if (highest.getBidder().getId().equals(bidder.getId())) {
                throw new IllegalArgumentException("Bạn đang là người trả giá cao nhất");
            }
        });

        AuctionBid bid = new AuctionBid();
        bid.setAuction(auction);
        bid.setBidder(bidder);
        bid.setAmount(amount);
        auction.setCurrentPrice(amount);
        auctionRepository.save(auction);
        return auctionBidRepository.save(bid);
    }

    public List<AuctionBid> bidHistory(Long auctionId) {
        if (!auctionRepository.existsById(auctionId)) {
            throw new IllegalArgumentException("Không tìm thấy phiên đấu giá");
        }
        return auctionBidRepository.findByAuctionIdOrderByPlacedAtDesc(auctionId);
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
