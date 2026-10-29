package com.ares.controller;

import com.ares.dto.ApiResponse;
import com.ares.dto.ReturnDtos.CreateReturnRequest;
import com.ares.service.ReturnService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/returns")
public class ReturnApiController {
    private final ReturnService returns;

    public ReturnApiController(ReturnService returns) {
        this.returns = returns;
    }

    @GetMapping("/orders/{orderCode}")
    public ResponseEntity<ApiResponse> lookup(@PathVariable String orderCode, @RequestParam String phone) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Đã xác minh đơn hàng", returns.lookup(orderCode, phone)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @PostMapping("/orders/{orderCode}")
    public ResponseEntity<ApiResponse> create(@PathVariable String orderCode, @RequestBody CreateReturnRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Đã nhận yêu cầu đổi trả; cửa hàng sẽ xem xét và liên hệ bạn.", returns.create(orderCode, request)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        } catch (ArithmeticException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Giá trị đổi trả vượt giới hạn cho phép"));
        }
    }

    @GetMapping("/{requestCode}")
    public ResponseEntity<ApiResponse> track(@PathVariable String requestCode, @RequestParam String phone) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Đã tra cứu yêu cầu", returns.track(requestCode, phone)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }
}
