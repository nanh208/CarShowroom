package com.ares.service;

import com.ares.model.BroadcastMessage;
import com.ares.model.Promotion;
import com.ares.repository.BroadcastRepository;
import com.ares.repository.PromotionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MarketingService {

    private final PromotionRepository promotionRepository;
    private final BroadcastRepository broadcastRepository;

    public MarketingService(PromotionRepository promotionRepository, BroadcastRepository broadcastRepository) {
        this.promotionRepository = promotionRepository;
        this.broadcastRepository = broadcastRepository;
    }

    public Long calculateDiscount(String code, Long orderTotal) {
        Promotion promo = promotionRepository.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> new IllegalArgumentException("MÃ£ khuyáº¿n mÃ£i khÃ´ng tá»“n táº¡i hoáº·c Ä‘Ã£ háº¿t háº¡n"));

        LocalDateTime now = LocalDateTime.now();
        if (promo.getValidFrom() != null && now.isBefore(promo.getValidFrom())) {
            throw new IllegalArgumentException("ChÆ°Æ¡ng trÃ¬nh khuyáº¿n mÃ£i chÆ°a báº¯t Ä‘áº§u");
        }
        if (promo.getValidTo() != null && now.isAfter(promo.getValidTo())) {
            throw new IllegalArgumentException("MÃ£ khuyáº¿n mÃ£i Ä‘Ã£ háº¿t háº¡n");
        }
        if (promo.getMinOrderValue() != null && orderTotal < promo.getMinOrderValue()) {
            throw new IllegalArgumentException("ÄÆ¡n hÃ ng chÆ°a Ä‘áº¡t giÃ¡ trá»‹ tá»‘i thiá»ƒu " + promo.getMinOrderValue() + " â‚«");
        }

        long discount = 0;
        if ("PERCENTAGE".equalsIgnoreCase(promo.getDiscountType())) {
            discount = (orderTotal * promo.getDiscountValue()) / 100;
            if (promo.getMaxDiscount() != null && promo.getMaxDiscount() > 0 && discount > promo.getMaxDiscount()) {
                discount = promo.getMaxDiscount();
            }
        } else {
            discount = promo.getDiscountValue();
        }

        return Math.min(discount, orderTotal);
    }

    public BroadcastMessage createBroadcast(String title, String message, String target) {
        BroadcastMessage bm = new BroadcastMessage();
        bm.setTitle(title);
        bm.setMessage(message);
        bm.setTargetAudience(target != null ? target : "ALL");
        return broadcastRepository.save(bm);
    }

    public List<BroadcastMessage> getRecentBroadcasts() {
        return broadcastRepository.findAllByOrderByCreatedAtDesc();
    }
}
