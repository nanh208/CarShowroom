package com.ares.controller;

import com.ares.dto.ApiResponse;
import com.ares.model.BroadcastMessage;
import com.ares.service.MarketingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/marketing")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class MarketingApiController {

    private final MarketingService marketingService;

    public MarketingApiController(MarketingService marketingService) {
        this.marketingService = marketingService;
    }

    @PostMapping("/validate-coupon")
    public ResponseEntity<ApiResponse> validateCoupon(@RequestBody CouponRequest req) {
        try {
            Long discount = marketingService.calculateDiscount(req.code(), req.orderTotal());
            ApiResponse res = ApiResponse.ok("Ãp dá»¥ng mÃ£ Æ°u Ä‘Ã£i thÃ nh cÃ´ng");
            res.setData(Map.of("code", req.code(), "discount", discount));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/admin/broadcast")
    public ResponseEntity<ApiResponse> broadcast(@RequestBody BroadcastRequest req) {
        BroadcastMessage bm = marketingService.createBroadcast(req.title(), req.message(), req.target());
        ApiResponse res = ApiResponse.ok("ÄÃ£ phÃ¡t thÃ´ng bÃ¡o chiáº¿n dá»‹ch Marketing thÃ nh cÃ´ng");
        res.setData(bm);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/broadcasts")
    public ResponseEntity<ApiResponse> listBroadcasts() {
        ApiResponse res = ApiResponse.ok("ThÃ´ng bÃ¡o má»›i");
        res.setData(marketingService.getRecentBroadcasts());
        return ResponseEntity.ok(res);
    }

    public record CouponRequest(String code, Long orderTotal) {}
    public record BroadcastRequest(String title, String message, String target) {}
}
