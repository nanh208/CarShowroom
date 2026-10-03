package com.ares.controller;

import com.ares.dto.ApiResponse;
import com.ares.dto.ShopDtos.CategoryDto;
import com.ares.dto.ShopDtos.ReviewDto;
import com.ares.model.Car;
import com.ares.service.CarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class CarApiController {

    private final CarService carService;

    @Autowired
    public CarApiController(CarService carService) {
        this.carService = carService;
    }

    /**
     * GET /api/cars
     * Filter by section (BEST_SELLER, RC_HIGHLIGHTS, MODEL_HIGHLIGHTS)
     * or category (RC_CARS, MODEL_CARS) or keyword search
     */
    @GetMapping("/cars")
    public ResponseEntity<ApiResponse> getCars(
            @RequestParam(required = false) String section,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {
        List<Car> cars = carService.getAllCars(section, category, search);
        return ResponseEntity.ok(ApiResponse.ok("Danh sách siêu xe mô hình", cars));
    }

    /**
     * GET /api/cars/{id}
     * Get specific car details for showroom modal lightbox
     */
    @GetMapping("/cars/{id}")
    public ResponseEntity<ApiResponse> getCarById(@PathVariable Long id) {
        Optional<Car> carOpt = carService.getCarById(id);
        if (carOpt.isPresent()) {
            return ResponseEntity.ok(ApiResponse.ok("Chi tiết siêu xe", carOpt.get()));
        } else {
            return ResponseEntity.status(404).body(ApiResponse.error("Không tìm thấy siêu xe với ID: " + id));
        }
    }

    /**
     * GET /api/categories
     * List of categories (RC Cars, Model Cars, Batteries, Controllers, Other Parts)
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse> getCategories() {
        List<CategoryDto> categories = carService.getCategories();
        return ResponseEntity.ok(ApiResponse.ok("Danh sách danh mục sản phẩm", categories));
    }

    /**
     * GET /api/reviews
     * List of customer reviews
     */
    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse> getReviews() {
        List<ReviewDto> reviews = carService.getReviews();
        return ResponseEntity.ok(ApiResponse.ok("Đánh giá từ khách hàng", reviews));
    }

    /**
     * GET /api/promotions
     * Promotion banners and campaigns
     */
    @GetMapping("/promotions")
    public ResponseEntity<ApiResponse> getPromotions() {
        Map<String, Object> promo = new HashMap<>();
        promo.put("title", "Mid-Autumn Festival Sale");
        promo.put("subtitle", "Sale up to 40% OFF");
        promo.put("heroBanner", "/images/cars/huracan-banner-exact.png");
        promo.put("saleCarImage", "/images/cars/huracan-dark.png");
        return ResponseEntity.ok(ApiResponse.ok("Thông tin chương trình khuyến mãi", promo));
    }
}
