package com.ares.controller;

import com.ares.dto.ApiResponse;
import com.ares.model.Auction;
import com.ares.model.AuctionStatus;
import com.ares.service.AuctionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auctions")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class AuctionApiController {
    private final AuctionService auctionService;

    public AuctionApiController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> list(@RequestParam(required = false) AuctionStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Danh sách phiên đấu giá", auctionService.findAll(status)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> detail(@PathVariable Long id) {
        return auctionService.findById(id)
                .map(auction -> ResponseEntity.ok(ApiResponse.ok("Chi tiết phiên đấu giá", auction)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("Không tìm thấy phiên đấu giá")));
    }
}
