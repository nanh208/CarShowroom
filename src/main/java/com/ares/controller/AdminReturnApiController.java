package com.ares.controller;

import com.ares.dto.ApiResponse;
import com.ares.dto.ReturnDtos.UpdateReturnStatusRequest;
import com.ares.service.ReturnService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.CacheControl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/admin/returns")
public class AdminReturnApiController {
    private final ReturnService returns;
    private final String adminToken;

    public AdminReturnApiController(ReturnService returns, @Value("${ares.admin.token:}") String adminToken) {
        this.returns = returns;
        this.adminToken = adminToken;
    }

    @GetMapping
    public ResponseEntity<ApiResponse> list(@RequestHeader(value = "X-Admin-Token", required = false) String suppliedToken) {
        ResponseEntity<ApiResponse> denied = authorize(suppliedToken);
        if (denied != null) return denied;
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok("Danh sách yêu cầu đổi trả", returns.listForAdmin()));
    }

    @PatchMapping("/{requestCode}")
    public ResponseEntity<ApiResponse> update(@PathVariable String requestCode,
            @RequestHeader(value = "X-Admin-Token", required = false) String suppliedToken,
            @RequestBody UpdateReturnStatusRequest update) {
        ResponseEntity<ApiResponse> denied = authorize(suppliedToken);
        if (denied != null) return denied;
        try {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                    .body(ApiResponse.ok("Đã cập nhật yêu cầu đổi trả", returns.updateStatus(requestCode, update)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    private ResponseEntity<ApiResponse> authorize(String suppliedToken) {
        if (adminToken == null || adminToken.length() < 32) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("Hãy cấu hình ARES_ADMIN_TOKEN dài ít nhất 32 ký tự trên máy chủ"));
        }
        if (suppliedToken == null || !MessageDigest.isEqual(adminToken.getBytes(StandardCharsets.UTF_8),
                suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Mã quản trị không hợp lệ"));
        }
        return null;
    }
}
