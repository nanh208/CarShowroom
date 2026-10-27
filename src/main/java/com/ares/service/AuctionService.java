package com.ares.service;

import com.ares.model.Auction;
import com.ares.model.AuctionBid;
import com.ares.model.AuctionStatus;
import com.ares.model.User;
import com.ares.repository.AuctionBidRepository;
import com.ares.repository.AuctionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
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
        if (status == null) {
            return auctionRepository.findAll();
        }
        return auctionRepository.findByStatus(status);
    }

    public Optional<Auction> findById(Long id) {
        refreshTimedStatuses();
        return auctionRepository.findById(id);
    }

    @Transactional
    public synchronized AuctionBid placeBid(Long auctionId, User bidder, Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("GiÃ¡ Ä‘áº·t pháº£i lá»›n hÆ¡n 0");
        }

        refreshTimedStatuses();
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("KhÃ´ng tÃ¬m tháº¥y phiÃªn Ä‘áº¥u giÃ¡"));

        if (auction.getStatus() != AuctionStatus.LIVE) {
            throw new IllegalArgumentException("PhiÃªn Ä‘áº¥u giÃ¡ hiá»‡n khÃ´ng diá»…n ra");
        }

        long nextMinimum = auction.getNextMinBid();
        if (amount < nextMinimum) {
            throw new IllegalArgumentException("GiÃ¡ Ä‘áº·t tá»‘i thiá»ƒu lÃ  " + nextMinimum + " VNÄ");
        }

        auctionBidRepository.findFirstByAuctionIdOrderByAmountDesc(auctionId).ifPresent(highest -> {
            if (highest.getBidder().getId().equals(bidder.getId())) {
                throw new IllegalArgumentException("Báº¡n Ä‘ang lÃ  ngÆ°á»i tráº£ giÃ¡ cao nháº¥t");
            }
        });

        // Anti-sniping: náº¿u Ä‘áº·t giÃ¡ trong 60 giÃ¢y cuá»‘i, tá»± Ä‘á»™ng kÃ©o dÃ i thÃªm 2 phÃºt
        LocalDateTime now = LocalDateTime.now();
        if (Duration.between(now, auction.getEndsAt()).getSeconds() < 60) {
            auction.setEndsAt(auction.getEndsAt().plusMinutes(2));
        }

        AuctionBid bid = new AuctionBid();
        bid.setAuction(auction);
        bid.setBidder(bidder);
        bid.setAmount(amount);
        bid.setPlacedAt(now);

        auction.setCurrentPrice(amount);
        auction.setBidCount((auction.getBidCount() == null ? 0 : auction.getBidCount()) + 1);
        auctionRepository.save(auction);
        return auctionBidRepository.save(bid);
    }

    public List<AuctionBid> bidHistory(Long auctionId) {
        if (!auctionRepository.existsById(auctionId)) {
            throw new IllegalArgumentException("KhÃ´ng tÃ¬m tháº¥y phiÃªn Ä‘áº¥u giÃ¡");
        }
        return auctionBidRepository.findByAuctionIdOrderByPlacedAtDesc(auctionId);
    }

    public void refreshTimedStatuses() {
        LocalDateTime now = LocalDateTime.now();
        for (Auction auction : auctionRepository.findAll()) {
            if (Boolean.TRUE.equals(auction.getCancelled())) continue;
            if (auction.getStartsAt() != null && auction.getEndsAt() != null) {
                if (now.isBefore(auction.getStartsAt())) {
                    auction.setStatus(AuctionStatus.UPCOMING);
                } else if (now.isBefore(auction.getEndsAt())) {
                    auction.setStatus(AuctionStatus.LIVE);
                } else {
                    auction.setStatus(AuctionStatus.ENDED);
                }
                auctionRepository.save(auction);
            }
        }
    }
}
