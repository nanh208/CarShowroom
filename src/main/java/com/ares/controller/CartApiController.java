package com.ares.controller;

import com.ares.dto.ApiResponse;
import com.ares.dto.ShopDtos.AddToCartRequest;
import com.ares.dto.ShopDtos.CartDto;
import com.ares.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class CartApiController {

    private final CartService cartService;

    @Autowired
    public CartApiController(CartService cartService) {
        this.cartService = cartService;
    }

    /**
     * GET /api/cart
     * Get current user's shopping cart
     */
    @GetMapping
    public ResponseEntity<ApiResponse> getCart(HttpSession session) {
        CartDto cart = cartService.getCart(session);
        return ResponseEntity.ok(ApiResponse.ok("Giỏ hàng hiện tại", cart));
    }

    /**
     * POST /api/cart/items
     * Add item to shopping cart
     */
    @PostMapping("/items")
    public ResponseEntity<ApiResponse> addToCart(@RequestBody AddToCartRequest req, HttpSession session) {
        if (req.getCarId() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng cung cấp mã sản phẩm carId!"));
        }
        int qty = req.getQuantity() > 0 ? req.getQuantity() : 1;
        CartDto cart = cartService.addToCart(session, req.getCarId(), qty);
        return ResponseEntity.ok(ApiResponse.ok("Đã thêm sản phẩm vào giỏ hàng thành công!", cart));
    }

    /**
     * PUT /api/cart/items/{carId}
     * Update quantity of an item
     */
    @PutMapping("/items/{carId}")
    public ResponseEntity<ApiResponse> updateQuantity(
            @PathVariable Long carId,
            @RequestParam int quantity,
            HttpSession session) {
        CartDto cart = cartService.updateItemQuantity(session, carId, quantity);
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật số lượng thành công!", cart));
    }

    /**
     * DELETE /api/cart/items/{carId}
     * Remove an item from shopping cart
     */
    @DeleteMapping("/items/{carId}")
    public ResponseEntity<ApiResponse> removeItem(@PathVariable Long carId, HttpSession session) {
        CartDto cart = cartService.removeFromCart(session, carId);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa sản phẩm khỏi giỏ hàng!", cart));
    }

    /**
     * DELETE /api/cart
     * Clear all items in cart
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse> clearCart(HttpSession session) {
        CartDto cart = cartService.clearCart(session);
        return ResponseEntity.ok(ApiResponse.ok("Đã làm trống giỏ hàng!", cart));
    }
}
