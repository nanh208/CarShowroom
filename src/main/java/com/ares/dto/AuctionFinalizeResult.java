package com.ares.dto;

import com.ares.model.User;
import java.time.LocalDateTime;

public record AuctionFinalizeResult(
    Long auctionId,
    String title,
    Long finalPrice,
    String winnerUsername,
    String orderCode,
    LocalDateTime finalizedAt
) {}
