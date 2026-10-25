package com.ares.controller;

import com.ares.model.Car;
import com.ares.repository.CarRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only bridge from the existing car catalog to the Vue product detail shape. */
@RestController
@RequestMapping("/api/products")
public class ProductApiController {
    private final CarRepository cars;

    public ProductApiController(CarRepository cars) {
        this.cars = cars;
    }

    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        Car car = cars.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy sản phẩm"));
        var related = car.getCategory() == null ? List.<Map<String, Object>>of()
            : cars.findByCategory(car.getCategory()).stream()
                .filter(other -> !id.equals(other.getId())).limit(4)
                .map(ProductApiController::product).toList();
        return Map.of("product", product(car), "related", related,
            "variants", List.of(), "flash", List.of());
    }

    private static Map<String, Object> product(Car car) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", car.getId());
        result.put("name", car.getName());
        result.put("sku", car.getCode());
        result.put("price", car.getPrice());
        result.put("oldPrice", car.getOldPrice());
        result.put("type", car.getCategory());
        result.put("scale", car.getScale());
        result.put("material", car.getMaterial());
        result.put("description", car.getDescription());
        result.put("rating", car.getRating());
        result.put("reviewCount", car.getReviewCount());
        result.put("sold", car.getSoldCount());
        result.put("inStock", Boolean.TRUE.equals(car.getInStock()));
        result.put("images", car.getImage() == null || car.getImage().isBlank()
            ? List.of() : List.of(car.getImage()));
        result.put("specs", car.getFeatures() == null ? Map.of() : Map.of("Features", car.getFeatures()));
        result.put("hasVariants", false);
        return result;
    }
}
