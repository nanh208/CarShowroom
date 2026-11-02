package com.ares.controller;

import com.ares.dto.ApiResponse;
import com.ares.dto.OrderDtos.CreateOrderRequest;
import com.ares.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderApiController {

    private final OrderService orderService;

    public OrderApiController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse> placeOrder(@RequestBody CreateOrderRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Đặt hàng thành công", orderService.place(request)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        } catch (ArithmeticException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Tổng giá trị đơn hàng vượt giới hạn cho phép"));
        }
    }
}
