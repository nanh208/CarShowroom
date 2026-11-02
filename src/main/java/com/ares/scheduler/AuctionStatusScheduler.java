package com.ares.scheduler;

import com.ares.model.Auction;
import com.ares.model.AuctionStatus;
import com.ares.repository.AuctionRepository;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@EnableScheduling
public class AuctionStatusScheduler {

    private final AuctionRepository auctionRepository;

    public AuctionStatusScheduler(AuctionRepository auctionRepository) {
        this.auctionRepository = auctionRepository;
    }

    @Scheduled(fixedRate = 10000)
    public void updateAuctionStatuses() {
        LocalDateTime now = LocalDateTime.now();
        List<Auction> auctions = auctionRepository.findAll();

        for (Auction a : auctions) {
            if (Boolean.TRUE.equals(a.getCancelled())) {
                continue;
            }

            AuctionStatus current = a.getStatus();
            if (current == AuctionStatus.UPCOMING && !now.isBefore(a.getStartsAt())) {
                if (now.isBefore(a.getEndsAt())) {
                    a.setStatus(AuctionStatus.LIVE);
                    auctionRepository.save(a);
                } else {
                    a.setStatus(AuctionStatus.ENDED);
                    auctionRepository.save(a);
                }
            } else if (current == AuctionStatus.LIVE && !now.isBefore(a.getEndsAt())) {
                a.setStatus(AuctionStatus.ENDED);
                auctionRepository.save(a);
            }
        }
    }
}
