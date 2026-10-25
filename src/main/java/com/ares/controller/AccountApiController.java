package com.ares.controller;

import com.ares.dto.ApiResponse;
import com.ares.dto.AuthDtos.UserDto;
import com.ares.model.User;
import com.ares.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.Set;

@RestController
@RequestMapping("/api/account")
public class AccountApiController {
    private static final Set<String> GENDERS = Set.of("Nam", "Nữ", "Khác", "Không xác định");

    private final UserRepository users;

    public AccountApiController(UserRepository users) {
        this.users = users;
    }

    public static class ProfileUpdateRequest {
        private String phone;
        private String avatarUrl;
        private Integer birthDay;
        private Integer birthMonth;
        private String gender;

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getAvatarUrl() { return avatarUrl; }
        public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
        public Integer getBirthDay() { return birthDay; }
        public void setBirthDay(Integer birthDay) { this.birthDay = birthDay; }
        public Integer getBirthMonth() { return birthMonth; }
        public void setBirthMonth(Integer birthMonth) { this.birthMonth = birthMonth; }
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse> profile(HttpSession session) {
        User sessionUser = (User) session.getAttribute("currentUser");
        if (sessionUser == null) return unauthorized();

        return users.findById(sessionUser.getId())
                .map(user -> ResponseEntity.ok(ApiResponse.ok("Thông tin hồ sơ", new UserDto(user))))
                .orElseGet(() -> unauthorized());
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse> updateProfile(@RequestBody ProfileUpdateRequest request, HttpSession session) {
        User sessionUser = (User) session.getAttribute("currentUser");
        if (sessionUser == null) return unauthorized();

        User user = users.findById(sessionUser.getId()).orElse(null);
        if (user == null) {
            session.invalidate();
            return unauthorized();
        }

        if (request.getPhone() != null) {
            String phone = request.getPhone().trim();
            if (!phone.isEmpty() && users.existsByPhone(phone)
                    && !phone.equals(user.getPhone())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Số điện thoại đã được sử dụng!"));
            }
            user.setPhone(phone.isEmpty() ? null : phone);
        }

        if (request.getAvatarUrl() != null) {
            String avatarUrl = request.getAvatarUrl().trim();
            if (avatarUrl.length() > 2048) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Đường dẫn ảnh đại diện quá dài"));
            }
            user.setAvatarUrl(avatarUrl.isEmpty() ? null : avatarUrl);
        }

        Integer day = request.getBirthDay();
        Integer month = request.getBirthMonth();
        if (day != null || month != null) {
            if (day == null || month == null || month < 1 || month > 12
                    || day < 1 || day > YearMonth.of(2024, month).lengthOfMonth()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Ngày sinh không hợp lệ!"));
            }
            user.setBirthDay(day);
            user.setBirthMonth(month);
        }

        if (request.getGender() != null) {
            String gender = request.getGender().trim();
            if (!GENDERS.contains(gender)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Giới tính không hợp lệ!"));
            }
            user.setGender(gender);
        }

        User saved = users.save(user);
        session.setAttribute("currentUser", saved);
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật hồ sơ", new UserDto(saved)));
    }

    private ResponseEntity<ApiResponse> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Vui lòng đăng nhập để xem hồ sơ!"));
    }
}
