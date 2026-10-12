package com.ares.controller;

import com.ares.dto.ApiResponse;
import com.ares.dto.AuthDtos.LoginRequest;
import com.ares.dto.AuthDtos.RegisterRequest;
import com.ares.dto.AuthDtos.ResetPasswordRequest;
import com.ares.dto.AuthDtos.SendCodeRequest;
import com.ares.dto.AuthDtos.UserDto;
import com.ares.dto.AuthDtos.VerifyCodeRequest;
import com.ares.model.User;
import com.ares.service.UserService;
import com.ares.service.VerificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
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

    /**
     * Endpoint for user login via JSON API.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@RequestBody LoginRequest req, HttpSession session) {
        if (req.getUsername() == null || req.getUsername().trim().isEmpty() ||
            req.getPassword() == null || req.getPassword().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lòng nhập tên đăng nhập và mật khẩu!"));
        }

        Optional<User> userOpt = userService.authenticate(req.getUsername(), req.getPassword());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            session.setAttribute("currentUser", user);
            UserDto userDto = new UserDto(user);
            return ResponseEntity.ok(ApiResponse.ok("Đăng nhập thành công!", userDto));
        } else {
            return ResponseEntity.status(401).body(ApiResponse.error("Tên đăng nhập hoặc mật khẩu không chính xác!"));
        }
    }

    /**
     * Endpoint to get currently logged in user info.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse> getCurrentUser(HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user != null) {
            return ResponseEntity.ok(ApiResponse.ok("Thông tin người dùng", new UserDto(user)));
        }
        return ResponseEntity.status(401).body(ApiResponse.error("Chưa đăng nhập!"));
    }

    /**
     * Endpoint to logout.
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(ApiResponse.ok("Đã đăng xuất an toàn!"));
    }

    @GetMapping("/logout")
    public ResponseEntity<ApiResponse> logoutGet(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(ApiResponse.ok("Đã đăng xuất an toàn!"));
    }
}
