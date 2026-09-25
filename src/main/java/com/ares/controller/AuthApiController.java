package com.ares.controller;

import com.ares.dto.ApiResponse;
import com.ares.dto.AuthDtos.RegisterRequest;
import com.ares.dto.AuthDtos.ResetPasswordRequest;
import com.ares.dto.AuthDtos.SendCodeRequest;
import com.ares.dto.AuthDtos.VerifyCodeRequest;
import com.ares.service.UserService;
import com.ares.service.VerificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final VerificationService verificationService;
    private final UserService userService;

    @Autowired
    public AuthApiController(VerificationService verificationService, UserService userService) {
        this.verificationService = verificationService;
        this.userService = userService;
    }

    /**
     * Endpoint to send OTP or Gmail verification code.
     */
    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse> sendCode(@RequestBody SendCodeRequest req) {
        if (req.getTarget() == null || req.getTarget().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng cung cấp Email hoặc Số điện thoại!"));
        }

        try {
            boolean isEmail = req.isEmail() || req.getTarget().contains("@");
            String code = verificationService.generateAndSendCode(req.getTarget(), isEmail);
            String msg = isEmail 
                ? "Mã xác thực đã được gửi về Gmail của bạn." 
                : "Mã OTP đã được gửi về số điện thoại (Chế độ an toàn chống mất phí).";

            return ResponseEntity.ok(ApiResponse.ok(msg));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(429).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi khi tạo mã xác nhận: " + e.getMessage()));
        }
    }

    /**
     * Endpoint to verify code before advancing to Step 2.
     */
    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse> verifyCode(@RequestBody VerifyCodeRequest req) {
        if (req.getTarget() == null || req.getCode() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Thiếu thông tin xác thực!"));
        }

        boolean valid = verificationService.verifyCode(req.getTarget(), req.getCode());
        if (valid) {
            return ResponseEntity.ok(ApiResponse.ok("Mã xác nhận hợp lệ!"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Mã xác nhận không đúng hoặc đã hết hạn!"));
        }
    }

    /**
     * Endpoint to register user.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@RequestBody RegisterRequest req) {
        ApiResponse res = userService.register(req);
        if (res.isSuccess()) {
            return ResponseEntity.ok(res);
        } else {
            return ResponseEntity.badRequest().body(res);
        }
    }

    /**
     * Endpoint to reset password in Step 2.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse> resetPassword(@RequestBody ResetPasswordRequest req) {
        ApiResponse res = userService.resetPassword(req);
        if (res.isSuccess()) {
            return ResponseEntity.ok(res);
        } else {
            return ResponseEntity.badRequest().body(res);
        }
    }
}
