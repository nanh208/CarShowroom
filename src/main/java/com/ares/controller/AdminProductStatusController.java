package com.ares.controller;

import com.ares.dto.ApiResponse;
import com.ares.model.Car;
import com.ares.model.User;
import com.ares.model.UserRole;
import com.ares.repository.CarRepository;
import com.ares.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;

@Controller
public class AdminProductStatusController {
    private final CarRepository cars;
    private final UserRepository users;

    public AdminProductStatusController(CarRepository cars, UserRepository users) {
        this.cars = cars;
        this.users = users;
    }

    @GetMapping("/admin/products/status")
    public String page(HttpSession session) {
        requireStaff(session);
        return "admin-product-status";
    }

    @ResponseBody
    @GetMapping("/api/admin/products/status")
    public ResponseEntity<ApiResponse> list(HttpSession session) {
        ResponseEntity<ApiResponse> denied = authorizationFailure(session);
        if (denied != null) return denied;
        List<ProductStatus> result = cars.findAllByOrderByCategoryAscNameAsc().stream()
            .map(ProductStatus::from).toList();
        ApiResponse response = ApiResponse.ok("Danh sách trạng thái sản phẩm");
        response.setData(result);
        return ResponseEntity.ok(response);
    }

    @ResponseBody
    @PutMapping("/api/admin/products/status/{id}")
    public ResponseEntity<ApiResponse> update(@PathVariable Long id, @RequestBody StatusRequest request,
                                               HttpSession session) {
        ResponseEntity<ApiResponse> denied = authorizationFailure(session);
        if (denied != null) return denied;
        if (request == null || request.active() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Thiếu trạng thái bật/tắt"));
        }
        Car car = cars.findById(id).orElse(null);
        if (car == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Không tìm thấy sản phẩm"));
        car.setActive(request.active());
        Car saved = cars.save(car);
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật trạng thái sản phẩm", ProductStatus.from(saved)));
    }

    @ResponseBody
    @PostMapping("/api/admin/products/status/bulk")
    public ResponseEntity<ApiResponse> bulkUpdate(@RequestBody BulkStatusRequest request, HttpSession session) {
        ResponseEntity<ApiResponse> denied = authorizationFailure(session);
        if (denied != null) return denied;
        if (request == null || request.active() == null || request.ids() == null || request.ids().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Chọn ít nhất một sản phẩm và trạng thái cần áp dụng"));
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>(request.ids());
        if (ids.size() > 500 || ids.contains(null)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Danh sách sản phẩm không hợp lệ (tối đa 500 sản phẩm)"));
        }
        List<Car> selected = cars.findAllById(ids);
        selected.forEach(car -> car.setActive(request.active()));
        cars.saveAll(selected);
        ApiResponse response = ApiResponse.ok("Đã cập nhật trạng thái hàng loạt");
        response.setData(Map.of("updated", selected.size()));
        return ResponseEntity.ok(response);
    }

    private void requireStaff(HttpSession session) {
        ResponseEntity<ApiResponse> denied = authorizationFailure(session);
        if (denied != null) {
            throw new ResponseStatusException(HttpStatus.valueOf(denied.getStatusCode().value()));
        }
    }

    private ResponseEntity<ApiResponse> authorizationFailure(HttpSession session) {
        Object value = session.getAttribute("currentUser");
        if (!(value instanceof User sessionUser) || sessionUser.getId() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Vui lòng đăng nhập"));
        }
        User user = users.findById(sessionUser.getId()).orElse(null);
        if (user == null || (user.getRole() != UserRole.ADMIN && user.getRole() != UserRole.STAFF)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Bạn không có quyền quản lý sản phẩm"));
        }
        return null;
    }

    public record StatusRequest(Boolean active) {}
    public record BulkStatusRequest(List<Long> ids, Boolean active) {}

    public record ProductStatus(Long id, String name, String code, String category, String image, boolean active) {
        static ProductStatus from(Car car) {
            return new ProductStatus(car.getId(), car.getName(), car.getCode(), car.getCategory(), car.getImage(), car.isActive());
        }
    }
}
