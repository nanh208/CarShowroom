# setup_payloads.ps1 - Generates 20 distinct modular payloads for CarShowroom
$base = "c:\Users\koreka\CarShowroom"
$payloadsRoot = Join-Path $base "scripts\payloads"

if (!(Test-Path $payloadsRoot)) {
    New-Item -ItemType Directory -Force -Path $payloadsRoot | Out-Null
}

function Create-PayloadDir($dayNum) {
    $dir = Join-Path $payloadsRoot ("day{0:D2}" -f $dayNum)
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Force -Path $dir | Out-Null
    }
    return $dir
}

# --- DAY 01 ---
$d1 = Create-PayloadDir 1
$d1Model = Join-Path $d1 "src\main\java\com\ares\model"
$d1Dto = Join-Path $d1 "src\main\java\com\ares\dto"
New-Item -ItemType Directory -Force -Path $d1Model | Out-Null
New-Item -ItemType Directory -Force -Path $d1Dto | Out-Null

@'
package com.ares.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "auctions")
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String vehicleName;

    private String brandName;

    @Column(name = "model_year")
    private Integer modelYear;

    private String mileage;

    @Column(length = 3000)
    private String description;

    private String image;

    @ElementCollection(fetch = FetchType.EAGER)
    @OrderColumn
    private List<String> images = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @MapKeyColumn(name = "spec_key")
    @Column(name = "spec_value")
    private Map<String, String> specs = new LinkedHashMap<>();

    @Column(nullable = false)
    private Long startingPrice;

    @Column(nullable = false)
    private Long currentPrice;

    @Column(nullable = false)
    private Long minimumIncrement;

    private Integer bidCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status = AuctionStatus.UPCOMING;

    @Column(nullable = false)
    private LocalDateTime startsAt;

    @Column(nullable = false)
    private LocalDateTime endsAt;

    private Boolean cancelled = false;

    private Boolean finalized = false;

    @JsonIgnoreProperties({"password", "phone", "email"})
    @ManyToOne(fetch = FetchType.EAGER)
    private User winner;

    private String orderCode;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Auction() { }

    public Auction(String title, String vehicleName, String brandName, Integer modelYear,
                   String mileage, String description, String image, Long startingPrice,
                   Long minimumIncrement, LocalDateTime startsAt, LocalDateTime endsAt) {
        this.title = title;
        this.vehicleName = vehicleName;
        this.brandName = brandName;
        this.modelYear = modelYear;
        this.mileage = mileage;
        this.description = description;
        this.image = image;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.minimumIncrement = minimumIncrement;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }
    public String getBrandName() { return brandName; }
    public void setBrandName(String brandName) { this.brandName = brandName; }
    public Integer getModelYear() { return modelYear; }
    public void setModelYear(Integer modelYear) { this.modelYear = modelYear; }
    public String getMileage() { return mileage; }
    public void setMileage(String mileage) { this.mileage = mileage; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public Map<String, String> getSpecs() { return specs; }
    public void setSpecs(Map<String, String> specs) { this.specs = specs; }
    public Long getStartingPrice() { return startingPrice; }
    public void setStartingPrice(Long startingPrice) { this.startingPrice = startingPrice; }
    public Long getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(Long currentPrice) { this.currentPrice = currentPrice; }
    public Long getMinimumIncrement() { return minimumIncrement; }
    public void setMinimumIncrement(Long minimumIncrement) { this.minimumIncrement = minimumIncrement; }
    public Integer getBidCount() { return bidCount; }
    public void setBidCount(Integer bidCount) { this.bidCount = bidCount; }
    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime startsAt) { this.startsAt = startsAt; }
    public LocalDateTime getEndsAt() { return endsAt; }
    public void setEndsAt(LocalDateTime endsAt) { this.endsAt = endsAt; }
    public Boolean getCancelled() { return cancelled; }
    public void setCancelled(Boolean cancelled) { this.cancelled = cancelled; }
    public Boolean getFinalized() { return finalized; }
    public void setFinalized(Boolean finalized) { this.finalized = finalized; }
    public User getWinner() { return winner; }
    public void setWinner(User winner) { this.winner = winner; }
    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public long getNextMinBid() {
        return (bidCount == null || bidCount == 0) ? startingPrice : currentPrice + minimumIncrement;
    }
}
'@ | Set-Content (Join-Path $d1Model "Auction.java") -Encoding UTF8

@'
package com.ares.dto;

import com.ares.model.Auction;
import com.ares.model.AuctionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AuctionDto(
    Long id,
    String title,
    String vehicleName,
    String brandName,
    Integer modelYear,
    String mileage,
    String description,
    String image,
    List<String> images,
    Map<String, String> specs,
    Long startingPrice,
    Long currentPrice,
    Long minimumIncrement,
    Long nextMinBid,
    Integer bidCount,
    AuctionStatus status,
    LocalDateTime startsAt,
    LocalDateTime endsAt,
    Boolean finalized,
    String winnerName,
    String orderCode
) {
    public static AuctionDto from(Auction a) {
        String winner = a.getWinner() != null ? a.getWinner().getUsername() : null;
        return new AuctionDto(
            a.getId(), a.getTitle(), a.getVehicleName(), a.getBrandName(),
            a.getModelYear(), a.getMileage(), a.getDescription(), a.getImage(),
            a.getImages(), a.getSpecs(), a.getStartingPrice(), a.getCurrentPrice(),
            a.getMinimumIncrement(), a.getNextMinBid(), a.getBidCount(),
            a.getStatus(), a.getStartsAt(), a.getEndsAt(), a.getFinalized(),
            winner, a.getOrderCode()
        );
    }
}
'@ | Set-Content (Join-Path $d1Dto "AuctionDto.java") -Encoding UTF8

# --- DAY 02 ---
$d2 = Create-PayloadDir 2
$d2Scheduler = Join-Path $d2 "src\main\java\com\ares\scheduler"
New-Item -ItemType Directory -Force -Path $d2Scheduler | Out-Null

@'
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
'@ | Set-Content (Join-Path $d2Scheduler "AuctionStatusScheduler.java") -Encoding UTF8

# --- DAY 03 ---
$d3 = Create-PayloadDir 3
$d3Service = Join-Path $d3 "src\main\java\com\ares\service"
New-Item -ItemType Directory -Force -Path $d3Service | Out-Null

@'
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
            throw new IllegalArgumentException("Giá đặt phải lớn hơn 0");
        }

        refreshTimedStatuses();
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên đấu giá"));

        if (auction.getStatus() != AuctionStatus.LIVE) {
            throw new IllegalArgumentException("Phiên đấu giá hiện không diễn ra");
        }

        long nextMinimum = auction.getNextMinBid();
        if (amount < nextMinimum) {
            throw new IllegalArgumentException("Giá đặt tối thiểu là " + nextMinimum + " VNĐ");
        }

        auctionBidRepository.findFirstByAuctionIdOrderByAmountDesc(auctionId).ifPresent(highest -> {
            if (highest.getBidder().getId().equals(bidder.getId())) {
                throw new IllegalArgumentException("Bạn đang là người trả giá cao nhất");
            }
        });

        // Anti-sniping: nếu đặt giá trong 60 giây cuối, tự động kéo dài thêm 2 phút
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
            throw new IllegalArgumentException("Không tìm thấy phiên đấu giá");
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
'@ | Set-Content (Join-Path $d3Service "AuctionService.java") -Encoding UTF8

# --- DAY 04 ---
$d4 = Create-PayloadDir 4
$d4Service = Join-Path $d4 "src\main\java\com\ares\service"
$d4Dto = Join-Path $d4 "src\main\java\com\ares\dto"
New-Item -ItemType Directory -Force -Path $d4Service | Out-Null
New-Item -ItemType Directory -Force -Path $d4Dto | Out-Null

@'
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
'@ | Set-Content (Join-Path $d4Dto "AuctionFinalizeResult.java") -Encoding UTF8

@'
package com.ares.service;

import com.ares.dto.AuctionFinalizeResult;
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
import java.time.format.DateTimeFormatter;
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
        if (status == null) return auctionRepository.findAll();
        return auctionRepository.findByStatus(status);
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

        long nextMinimum = auction.getNextMinBid();
        if (amount < nextMinimum) {
            throw new IllegalArgumentException("Giá đặt tối thiểu là " + nextMinimum + " VNĐ");
        }

        auctionBidRepository.findFirstByAuctionIdOrderByAmountDesc(auctionId).ifPresent(highest -> {
            if (highest.getBidder().getId().equals(bidder.getId())) {
                throw new IllegalArgumentException("Bạn đang là người trả giá cao nhất");
            }
        });

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

    @Transactional
    public synchronized AuctionFinalizeResult finalizeAuction(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy phiên đấu giá"));

        if (Boolean.TRUE.equals(auction.getFinalized())) {
            throw new IllegalStateException("Phiên đấu giá đã được chốt trước đó");
        }

        Optional<AuctionBid> highestBid = auctionBidRepository.findFirstByAuctionIdOrderByAmountDesc(auctionId);
        if (highestBid.isPresent()) {
            AuctionBid top = highestBid.get();
            auction.setWinner(top.getBidder());
            String dateTag = DateTimeFormatter.ofPattern("yyyyMMdd").format(LocalDateTime.now());
            auction.setOrderCode("AUC-" + dateTag + "-" + String.format("%04d", auction.getId()));
        }

        auction.setStatus(AuctionStatus.ENDED);
        auction.setFinalized(true);
        auctionRepository.save(auction);

        String winnerName = auction.getWinner() != null ? auction.getWinner().getUsername() : "Không có người đặt giá";
        return new AuctionFinalizeResult(auction.getId(), auction.getTitle(), auction.getCurrentPrice(), winnerName, auction.getOrderCode(), LocalDateTime.now());
    }

    public List<AuctionBid> bidHistory(Long auctionId) {
        if (!auctionRepository.existsById(auctionId)) {
            throw new IllegalArgumentException("Không tìm thấy phiên đấu giá");
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
'@ | Set-Content (Join-Path $d4Service "AuctionService.java") -Encoding UTF8

# --- DAY 05 ---
$d5 = Create-PayloadDir 5
$d5Ctrl = Join-Path $d5 "src\main\java\com\ares\controller"
New-Item -ItemType Directory -Force -Path $d5Ctrl | Out-Null

@'
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
        ApiResponse res = ApiResponse.ok("Danh sách toàn bộ phiên đấu giá");
        res.setData(dtoList);
        return ResponseEntity.ok(res);
    }

    @PostMapping
    public ResponseEntity<ApiResponse> createAuction(@RequestBody CreateAuctionRequest req) {
        if (req.title() == null || req.startingPrice() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Thiếu thông tin bắt buộc"));
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
        ApiResponse res = ApiResponse.ok("Tạo phiên đấu giá thành công");
        res.setData(AuctionDto.from(saved));
        return ResponseEntity.ok(res);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse> cancelAuction(@PathVariable Long id) {
        return auctionRepository.findById(id).map(a -> {
            a.setCancelled(true);
            a.setStatus(AuctionStatus.ENDED);
            auctionRepository.save(a);
            return ResponseEntity.ok(ApiResponse.ok("Đã hủy phiên đấu giá thành công"));
        }).orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("Không tìm thấy phiên đấu giá")));
    }

    @PostMapping("/{id}/finalize")
    public ResponseEntity<ApiResponse> finalizeAuction(@PathVariable Long id) {
        try {
            var result = auctionService.finalizeAuction(id);
            ApiResponse res = ApiResponse.ok("Chốt phiên đấu giá thành công");
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
'@ | Set-Content (Join-Path $d5Ctrl "AuctionAdminApiController.java") -Encoding UTF8

# --- DAY 06 ---
$d6 = Create-PayloadDir 6
$d6Tpl = Join-Path $d6 "src\main\resources\templates"
$d6Ctrl = Join-Path $d6 "src\main\java\com\ares\controller"
New-Item -ItemType Directory -Force -Path $d6Tpl | Out-Null
New-Item -ItemType Directory -Force -Path $d6Ctrl | Out-Null

@'
package com.ares.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AuctionViewController {

    @GetMapping("/auctions")
    public String auctionsPage() {
        return "auctions";
    }

    @GetMapping("/auctions/{id}")
    public String auctionDetailPage(@PathVariable Long id, Model model) {
        model.addAttribute("auctionId", id);
        return "auction-detail";
    }

    @GetMapping("/admin/auctions")
    public String adminAuctionsPage() {
        return "admin-auctions";
    }
}
'@ | Set-Content (Join-Path $d6Ctrl "AuctionViewController.java") -Encoding UTF8

@'
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Đấu Giá Xe Trực Tuyến - Ares Auto Showroom</title>
    <link rel="stylesheet" th:href="@{/css/auth.css}">
    <style>
        .auction-container { max-width: 1200px; margin: 30px auto; padding: 0 20px; font-family: 'Segoe UI', sans-serif; color: #f1f5f9; }
        .hero-banner { background: linear-gradient(135deg, #1e293b, #0f172a); border: 1px solid #334155; border-radius: 20px; padding: 40px; margin-bottom: 30px; text-align: center; }
        .hero-banner h1 { font-size: 2.4rem; color: #38bdf8; margin-bottom: 12px; }
        .hero-banner p { font-size: 1.1rem; color: #94a3b8; }
        .filter-tabs { display: flex; gap: 12px; justify-content: center; margin-bottom: 30px; }
        .tab-btn { background: #1e293b; color: #94a3b8; border: 1px solid #334155; padding: 10px 24px; border-radius: 12px; cursor: pointer; font-weight: 600; transition: all 0.2s; }
        .tab-btn.active, .tab-btn:hover { background: #0284c7; color: #fff; border-color: #38bdf8; }
        .auction-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(350px, 1fr)); gap: 24px; }
        .auction-card { background: #1e293b; border: 1px solid #334155; border-radius: 18px; overflow: hidden; transition: transform 0.2s, box-shadow 0.2s; display: flex; flex-direction: column; }
        .auction-card:hover { transform: translateY(-4px); box-shadow: 0 12px 30px rgba(0,0,0,0.5); }
        .card-img-wrap { position: relative; height: 220px; background: #0f172a; }
        .card-img { width: 100%; height: 100%; object-fit: cover; }
        .badge-status { position: absolute; top: 14px; left: 14px; padding: 6px 14px; border-radius: 8px; font-size: 0.8rem; font-weight: 700; text-transform: uppercase; }
        .badge-live { background: #ef4444; color: #fff; animation: pulse 2s infinite; }
        .badge-upcoming { background: #eab308; color: #000; }
        .badge-ended { background: #64748b; color: #fff; }
        .card-body { padding: 20px; flex: 1; display: flex; flex-direction: column; }
        .card-title { font-size: 1.25rem; font-weight: 700; color: #f8fafc; margin-bottom: 10px; }
        .price-box { background: rgba(15,23,42,0.6); padding: 12px; border-radius: 10px; margin: 15px 0; border: 1px solid #334155; }
        .price-label { font-size: 0.85rem; color: #94a3b8; }
        .price-val { font-size: 1.4rem; font-weight: 800; color: #38bdf8; }
        .btn-bid-now { display: block; text-align: center; background: #0284c7; color: #fff; padding: 12px; border-radius: 10px; text-decoration: none; font-weight: 700; margin-top: auto; }
        .btn-bid-now:hover { background: #0369a1; }
        @keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.7; } }
    </style>
</head>
<body style="background:#090d16;">
    <div class="auction-container">
        <div class="hero-banner">
            <h1>SÀN ĐẤU GIÁ XE ĐỘC BẢN & HIẾM</h1>
            <p>Khám phá và tham gia trả giá những siêu phẩm ô tô giới hạn cùng Ares Auto Showroom</p>
        </div>

        <div class="filter-tabs">
            <button class="tab-btn active" onclick="loadAuctions('')">Tất cả</button>
            <button class="tab-btn" onclick="loadAuctions('LIVE')">Đang diễn ra (LIVE)</button>
            <button class="tab-btn" onclick="loadAuctions('UPCOMING')">Sắp mở</button>
            <button class="tab-btn" onclick="loadAuctions('ENDED')">Đã chốt</button>
        </div>

        <div id="auctionGrid" class="auction-grid">
            <!-- Render via JS API -->
        </div>
    </div>

    <script>
        async function loadAuctions(status) {
            const url = status ? `/api/auctions?status=${status}` : '/api/auctions';
            try {
                const res = await fetch(url);
                const data = await res.json();
                const list = data.data || [];
                const grid = document.getElementById('auctionGrid');
                grid.innerHTML = '';

                if (list.length === 0) {
                    grid.innerHTML = '<p style="text-align:center;grid-column:1/-1;color:#94a3b8;">Hiện chưa có phiên đấu giá nào trong mục này.</p>';
                    return;
                }

                list.forEach(a => {
                    const statusClass = a.status === 'LIVE' ? 'badge-live' : (a.status === 'UPCOMING' ? 'badge-upcoming' : 'badge-ended');
                    const statusText = a.status === 'LIVE' ? 'Đang diễn ra' : (a.status === 'UPCOMING' ? 'Sắp mở' : 'Đã kết thúc');
                    const imgUrl = a.image || 'https://images.unsplash.com/photo-1617788138017-80ad40651399?auto=format&fit=crop&w=800&q=80';
                    const priceFormatted = new Intl.NumberFormat('vi-VN').format(a.currentPrice || a.startingPrice) + ' ₫';

                    grid.innerHTML += `
                        <div class="auction-card">
                            <div class="card-img-wrap">
                                <img src="${imgUrl}" alt="${a.title}" class="card-img">
                                <span class="badge-status ${statusClass}">${statusText}</span>
                            </div>
                            <div class="card-body">
                                <h3 class="card-title">${a.title}</h3>
                                <p style="color:#94a3b8;font-size:0.9rem;">${a.brandName || ''} • Năm ${a.modelYear || 2024} • ${a.mileage || 'Mới 100%'}</p>
                                <div class="price-box">
                                    <div class="price-label">Giá hiện tại</div>
                                    <div class="price-val">${priceFormatted}</div>
                                </div>
                                <a href="/auctions/${a.id}" class="btn-bid-now">Tham gia đấu giá</a>
                            </div>
                        </div>
                    `;
                });
            } catch(e) {
                console.error(e);
            }
        }
        document.addEventListener('DOMContentLoaded', () => loadAuctions(''));
    </script>
</body>
</html>
'@ | Set-Content (Join-Path $d6Tpl "auctions.html") -Encoding UTF8

# --- DAY 07 ---
$d7 = Create-PayloadDir 7
$d7Tpl = Join-Path $d7 "src\main\resources\templates"
New-Item -ItemType Directory -Force -Path $d7Tpl | Out-Null

@'
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chi Tiết Phiên Đấu Giá - Ares Auto</title>
    <style>
        body { background: #090d16; color: #f1f5f9; font-family: 'Segoe UI', sans-serif; margin: 0; padding: 20px; }
        .detail-wrap { max-width: 1100px; margin: 0 auto; display: grid; grid-template-columns: 1.2fr 1fr; gap: 30px; }
        .car-gallery { background: #1e293b; border-radius: 16px; overflow: hidden; border: 1px solid #334155; }
        .main-car-img { width: 100%; height: 380px; object-fit: cover; }
        .bid-panel { background: #1e293b; border-radius: 16px; padding: 25px; border: 1px solid #334155; }
        .price-highlight { font-size: 2.2rem; color: #38bdf8; font-weight: 800; margin: 10px 0; }
        .input-bid { width: 100%; box-sizing: border-box; background: #0f172a; border: 1px solid #475569; color: #fff; padding: 14px; border-radius: 10px; font-size: 1.2rem; font-weight: 700; margin-bottom: 12px; }
        .btn-quick { background: #334155; color: #94a3b8; border: none; padding: 8px 14px; border-radius: 8px; cursor: pointer; font-weight: 600; margin-right: 6px; }
        .btn-quick:hover { background: #475569; color: #fff; }
        .btn-submit-bid { width: 100%; background: #0284c7; color: #fff; border: none; padding: 14px; border-radius: 10px; font-size: 1.1rem; font-weight: 700; cursor: pointer; margin-top: 15px; }
        .btn-submit-bid:hover { background: #0369a1; }
        .history-card { margin-top: 25px; background: #0f172a; border-radius: 12px; padding: 15px; border: 1px solid #334155; }
        .history-item { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #1e293b; font-size: 0.95rem; }
    </style>
</head>
<body>
    <div class="detail-wrap">
        <div>
            <div class="car-gallery">
                <img id="carImg" src="" alt="Xe đấu giá" class="main-car-img">
            </div>
            <div style="margin-top:20px;background:#1e293b;padding:20px;border-radius:16px;border:1px solid #334155;">
                <h3 style="color:#38bdf8;margin-top:0;">Mô tả chi tiết</h3>
                <p id="carDesc" style="color:#cbd5e1;line-height:1.6;"></p>
            </div>
        </div>

        <div class="bid-panel">
            <h2 id="auctionTitle" style="margin-top:0;font-size:1.6rem;">Đang tải...</h2>
            <div style="color:#94a3b8;">Giá sàn hiện tại:</div>
            <div id="currentPrice" class="price-highlight">0 ₫</div>
            <div id="nextMinText" style="color:#38bdf8;font-size:0.95rem;margin-bottom:15px;">Mức giá tối thiểu kế tiếp: 0 ₫</div>

            <div style="margin-bottom:12px;">
                <span style="font-size:0.9rem;color:#94a3b8;">Cộng nhanh:</span>
                <button class="btn-quick" onclick="addQuick(5000000)">+5 Triệu</button>
                <button class="btn-quick" onclick="addQuick(10000000)">+10 Triệu</button>
                <button class="btn-quick" onclick="addQuick(20000000)">+20 Triệu</button>
            </div>

            <input type="number" id="bidInput" class="input-bid" placeholder="Nhập mức giá bạn muốn đặt">
            <button class="btn-submit-bid" onclick="placeBid()">ĐẶT GIÁ NGAY</button>
            <div id="bidMsg" style="margin-top:10px;font-weight:600;"></div>

            <div class="history-card">
                <h4 style="margin:0 0 10px 0;color:#94a3b8;">Lịch sử trả giá</h4>
                <div id="bidHistoryList">Đang tải lịch sử...</div>
            </div>
        </div>
    </div>

    <script th:inline="javascript">
        const auctionId = /*[[${auctionId}]]*/ 1;
        let nextMin = 0;

        async function loadDetail() {
            try {
                const res = await fetch(`/api/auctions/${auctionId}`);
                const resData = await res.json();
                const a = resData.data;
                document.getElementById('auctionTitle').innerText = a.title;
                document.getElementById('currentPrice').innerText = new Intl.NumberFormat('vi-VN').format(a.currentPrice) + ' ₫';
                nextMin = a.currentPrice + (a.minimumIncrement || 1000000);
                document.getElementById('nextMinText').innerText = `Mức giá tối thiểu kế tiếp: ${new Intl.NumberFormat('vi-VN').format(nextMin)} ₫`;
                document.getElementById('bidInput').value = nextMin;
                document.getElementById('carImg').src = a.image || 'https://images.unsplash.com/photo-1617788138017-80ad40651399?auto=format&fit=crop&w=800&q=80';
                document.getElementById('carDesc').innerText = a.description || 'Chiếc xe nguyên bản, đầy đủ sổ bảo hành chính hãng, sẵn sàng giao dịch.';
            } catch(e) { console.error(e); }
        }

        async function loadBids() {
            try {
                const res = await fetch(`/api/auctions/${auctionId}/bids`);
                const resData = await res.json();
                const list = resData.data || [];
                const container = document.getElementById('bidHistoryList');
                if (list.length === 0) {
                    container.innerHTML = '<span style="color:#64748b;">Chưa có lượt đặt giá nào.</span>';
                    return;
                }
                container.innerHTML = list.map(b => `
                    <div class="history-item">
                        <span>${b.bidder || 'Người tham gia'}</span>
                        <strong style="color:#38bdf8;">${new Intl.NumberFormat('vi-VN').format(b.amount)} ₫</strong>
                    </div>
                `).join('');
            } catch(e) { console.error(e); }
        }

        function addQuick(val) {
            const inp = document.getElementById('bidInput');
            inp.value = Number(inp.value || nextMin) + val;
        }

        async function placeBid() {
            const amount = document.getElementById('bidInput').value;
            const msgBox = document.getElementById('bidMsg');
            msgBox.innerText = 'Đang gửi...';
            try {
                const res = await fetch(`/api/auctions/${auctionId}/bids`, {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                    body: JSON.stringify({ amount: Number(amount) })
                });
                const result = await res.json();
                if (result.success) {
                    msgBox.style.color = '#4ade80';
                    msgBox.innerText = 'Đặt giá thành công!';
                    loadDetail();
                    loadBids();
                } else {
                    msgBox.style.color = '#f87171';
                    msgBox.innerText = result.message || 'Lỗi đặt giá';
                }
            } catch(e) {
                msgBox.style.color = '#f87171';
                msgBox.innerText = 'Vui lòng đăng nhập để tham gia đấu giá';
            }
        }

        loadDetail();
        loadBids();
        setInterval(loadBids, 3000);
    </script>
</body>
</html>
'@ | Set-Content (Join-Path $d7Tpl "auction-detail.html") -Encoding UTF8

# --- DAY 08 ---
$d8 = Create-PayloadDir 8
$d8Tpl = Join-Path $d8 "src\main\resources\templates"
New-Item -ItemType Directory -Force -Path $d8Tpl | Out-Null

@'
<!DOCTYPE html>
<html lang="vi" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Quản Trị Đấu Giá Xe - Admin Ares</title>
    <style>
        body { background: #0f172a; color: #f1f5f9; font-family: 'Segoe UI', sans-serif; padding: 25px; }
        .admin-table { width: 100%; border-collapse: collapse; margin-top: 20px; background: #1e293b; border-radius: 12px; overflow: hidden; }
        .admin-table th, .admin-table td { padding: 14px 18px; text-align: left; border-bottom: 1px solid #334155; }
        .admin-table th { background: #1e293b; color: #94a3b8; font-weight: 600; }
        .btn-action { padding: 6px 12px; border-radius: 6px; border: none; cursor: pointer; font-weight: 600; margin-right: 5px; }
        .btn-cancel { background: #ef4444; color: #fff; }
        .btn-finalize { background: #10b981; color: #fff; }
    </style>
</head>
<body>
    <h2>BẢNG ĐIỀU KHIỂN QUẢN TRỊ ĐẤU GIÁ</h2>
    <table class="admin-table">
        <thead>
            <tr>
                <th>ID</th>
                <th>Tên Xe</th>
                <th>Giá Khởi Điểm</th>
                <th>Giá Hiện Tại</th>
                <th>Lượt Bid</th>
                <th>Trạng Thái</th>
                <th>Thao Tác</th>
            </tr>
        </thead>
        <tbody id="adminAuctionBody"></tbody>
    </table>

    <script>
        async function loadAdminAuctions() {
            const res = await fetch('/api/admin/auctions');
            const data = await res.json();
            const list = data.data || [];
            const tbody = document.getElementById('adminAuctionBody');
            tbody.innerHTML = list.map(a => `
                <tr>
                    <td>#${a.id}</td>
                    <td><strong>${a.title}</strong></td>
                    <td>${new Intl.NumberFormat('vi-VN').format(a.startingPrice)} ₫</td>
                    <td style="color:#38bdf8;font-weight:700;">${new Intl.NumberFormat('vi-VN').format(a.currentPrice)} ₫</td>
                    <td>${a.bidCount || 0}</td>
                    <td><span style="font-weight:700;">${a.status}</span></td>
                    <td>
                        <button class="btn-action btn-finalize" onclick="finalize(${a.id})">Chốt phiên</button>
                        <button class="btn-action btn-cancel" onclick="cancel(${a.id})">Hủy</button>
                    </td>
                </tr>
            `).join('');
        }

        async function finalize(id) {
            if (!confirm('Xác nhận chốt phiên đấu giá #' + id + '?')) return;
            const res = await fetch(`/api/admin/auctions/${id}/finalize`, {method: 'POST'});
            const r = await res.json();
            alert(r.message);
            loadAdminAuctions();
        }

        async function cancel(id) {
            if (!confirm('Xác nhận hủy phiên #' + id + '?')) return;
            const res = await fetch(`/api/admin/auctions/${id}/cancel`, {method: 'POST'});
            const r = await res.json();
            alert(r.message);
            loadAdminAuctions();
        }

        loadAdminAuctions();
    </script>
</body>
</html>
'@ | Set-Content (Join-Path $d8Tpl "admin-auctions.html") -Encoding UTF8

Write-Host "Setup initial Days 1-8 completed."
