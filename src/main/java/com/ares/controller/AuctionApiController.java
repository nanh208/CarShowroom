package com.ares.controller;

import com.ares.dto.ApiResponse;
import com.ares.model.Auction;
import com.ares.model.AuctionBid;
import com.ares.model.AuctionStatus;
import com.ares.model.User;
import com.ares.service.AuctionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/{id}/bids")
    public ResponseEntity<ApiResponse> bidHistory(@PathVariable Long id) {
        try {
            List<Map<String, Object>> bids = auctionService.bidHistory(id).stream()
                    .map(this::bidResponse)
                    .toList();
            return ResponseEntity.ok(ApiResponse.ok("Lịch sử trả giá", bids));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(404).body(ApiResponse.error(exception.getMessage()));
        }
    }

    @PostMapping("/{id}/bids")
    public ResponseEntity<ApiResponse> placeBid(@PathVariable Long id, @RequestBody BidRequest request,
                                                HttpSession session) {
        User bidder = (User) session.getAttribute("currentUser");
        if (bidder == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Vui lòng đăng nhập để trả giá"));
        }
        try {
            return ResponseEntity.ok(ApiResponse.ok("Đặt giá thành công", bidResponse(auctionService.placeBid(id, bidder, request.amount()))));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(ApiResponse.error(exception.getMessage()));
        }
    }

    private Map<String, Object> bidResponse(AuctionBid bid) {
        return Map.of(
                "id", bid.getId(),
                "amount", bid.getAmount(),
                "placedAt", bid.getPlacedAt(),
                "bidder", bid.getBidder().getUsername());
    }

    public record BidRequest(Long amount) { }
}
