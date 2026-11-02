package com.ares.controller;

import com.ares.dto.ApiResponse;
import com.ares.dto.AuctionDto;
import com.ares.model.Auction;
import com.ares.model.AuctionStatus;
import com.ares.repository.AuctionRepository;
import com.ares.service.AuctionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/auctions")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class AuctionAdminApiController {

    private final AuctionRepository auctionRepository;
    private final AuctionService auctionService;

    public AuctionAdminApiController(AuctionRepository auctionRepository, AuctionService auctionService) {
        this.auctionRepository = auctionRepository;
        this.auctionService = auctionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> listAll() {
        List<AuctionDto> dtoList = auctionRepository.findAll().stream()
                .map(AuctionDto::from)
                .toList();
        ApiResponse res = ApiResponse.ok("Danh sÃ¡ch toÃ n bá»™ phiÃªn Ä‘áº¥u giÃ¡");
        res.setData(dtoList);
        return ResponseEntity.ok(res);
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createAuction(@RequestBody CreateAuctionRequest req) {
        if (req.title() == null || req.startingPrice() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Thiáº¿u thÃ´ng tin báº¯t buá»™c"));
        }

        Auction a = new Auction();
        a.setTitle(req.title());
        a.setVehicleName(req.vehicleName() != null ? req.vehicleName() : req.title());
        a.setBrandName(req.brandName());
        a.setModelYear(req.modelYear());
        a.setMileage(req.mileage());
        a.setDescription(req.description());
        a.setImage(req.image());
        if (req.images() != null) a.setImages(req.images());
        if (req.specs() != null) a.setSpecs(req.specs());
        a.setStartingPrice(req.startingPrice());
        a.setCurrentPrice(req.startingPrice());
        a.setMinimumIncrement(req.minimumIncrement() != null ? req.minimumIncrement() : 1_000_000L);
        a.setStartsAt(req.startsAt() != null ? req.startsAt() : LocalDateTime.now());
        a.setEndsAt(req.endsAt() != null ? req.endsAt() : LocalDateTime.now().plusDays(3));
        a.setStatus(AuctionStatus.UPCOMING);

        Auction saved = auctionRepository.save(a);
        ApiResponse res = ApiResponse.ok("Táº¡o phiÃªn Ä‘áº¥u giÃ¡ thÃ nh cÃ´ng");
        res.setData(AuctionDto.from(saved));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse> cancelAuction(@PathVariable Long id) {
        return auctionRepository.findById(id).map(a -> {
            a.setCancelled(true);
            a.setStatus(AuctionStatus.ENDED);
            auctionRepository.save(a);
            return ResponseEntity.ok(ApiResponse.ok("ÄÃ£ há»§y phiÃªn Ä‘áº¥u giÃ¡ thÃ nh cÃ´ng"));
        }).orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("KhÃ´ng tÃ¬m tháº¥y phiÃªn Ä‘áº¥u giÃ¡")));
    }

    @PostMapping("/{id}/finalize")
    public ResponseEntity<ApiResponse> finalizeAuction(@PathVariable Long id) {
        try {
            var result = auctionService.finalizeAuction(id);
            ApiResponse res = ApiResponse.ok("Chá»‘t phiÃªn Ä‘áº¥u giÃ¡ thÃ nh cÃ´ng");
            res.setData(result);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    public record CreateAuctionRequest(
        String title, String vehicleName, String brandName, Integer modelYear,
        String mileage, String description, String image, List<String> images,
        java.util.Map<String, String> specs, Long startingPrice, Long minimumIncrement,
        LocalDateTime startsAt, LocalDateTime endsAt
    ) {}
}
