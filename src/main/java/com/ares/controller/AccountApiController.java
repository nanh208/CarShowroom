package com.ares.controller;

import com.ares.dto.ApiResponse;
import com.ares.dto.AuthDtos.UserDto;
import com.ares.model.Address;
import com.ares.model.User;
import com.ares.model.UserPreference;
import com.ares.repository.AddressRepository;
import com.ares.repository.UserRepository;
import com.ares.repository.UserPreferenceRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/account")
public class AccountApiController {
    private static final Set<String> GENDERS = Set.of("Nam", "Nữ", "Khác", "Không xác định");

    private final UserRepository users;
    private final AddressRepository addresses;
    private final UserPreferenceRepository preferences;

    public AccountApiController(UserRepository users, AddressRepository addresses, UserPreferenceRepository preferences) {
        this.users = users;
        this.addresses = addresses;
        this.preferences = preferences;
    }

    public record AddressRequest(String label, String receiverName, String phone, String line,
                                 String ward, String district, String city, Boolean isDefault) {}

    public record AddressView(Long id, String label, String receiverName, String phone, String line,
                              String ward, String district, String city, boolean isDefault) {
        static AddressView from(Address address) {
            return new AddressView(address.getId(), address.getLabel(), address.getReceiverName(), address.getPhone(),
                    address.getLine(), address.getWard(), address.getDistrict(), address.getCity(), address.isDefault());
        }
    }

    public static class ProfileUpdateRequest {
        private String fullName;
        private String phone;
        private String avatarUrl;
        private Integer birthDay;
        private Integer birthMonth;
        private String gender;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
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

        if (request.getFullName() != null) {
            String fullName = request.getFullName().trim();
            if (fullName.isEmpty() || fullName.length() > 100) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Tên hiển thị phải từ 1 đến 100 ký tự"));
            }
            user.setFullName(fullName);
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

    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse> addresses(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        List<AddressView> result = addresses.findByUser_IdOrderByIdDesc(user.getId()).stream()
                .map(AddressView::from).toList();
        return ResponseEntity.ok(ApiResponse.ok("Sổ địa chỉ", result));
    }

    @PostMapping("/addresses")
    @Transactional
    public ResponseEntity<ApiResponse> createAddress(@RequestBody AddressRequest request, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        String error = validateAddress(request);
        if (error != null) return ResponseEntity.badRequest().body(ApiResponse.error(error));

        List<Address> existing = addresses.findByUser_IdOrderByIdDesc(user.getId());
        Address address = new Address();
        address.setUser(user);
        applyAddress(address, request);
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault()) || existing.isEmpty();
        if (makeDefault) clearDefault(existing);
        address.setDefault(makeDefault);
        Address saved = addresses.save(address);
        return ResponseEntity.ok(ApiResponse.ok("Đã thêm địa chỉ", AddressView.from(saved)));
    }

    @PutMapping("/addresses/{id}")
    @Transactional
    public ResponseEntity<ApiResponse> updateAddress(@PathVariable Long id, @RequestBody AddressRequest request, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        String error = validateAddress(request);
        if (error != null) return ResponseEntity.badRequest().body(ApiResponse.error(error));

        Address address = addresses.findByIdAndUser_Id(id, user.getId()).orElse(null);
        if (address == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Không tìm thấy địa chỉ"));
        List<Address> existing = addresses.findByUser_IdOrderByIdDesc(user.getId());
        boolean wasDefault = address.isDefault();
        applyAddress(address, request);
        boolean makeDefault = Boolean.TRUE.equals(request.isDefault());
        if (makeDefault) {
            clearDefault(existing);
            address.setDefault(true);
        } else if (wasDefault) {
            Address replacement = existing.stream().filter(other -> !other.getId().equals(id)).findFirst().orElse(null);
            address.setDefault(replacement == null);
            if (replacement != null) replacement.setDefault(true);
            addresses.saveAll(existing);
        } else {
            address.setDefault(false);
        }
        Address saved = addresses.save(address);
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật địa chỉ", AddressView.from(saved)));
    }

    @DeleteMapping("/addresses/{id}")
    @Transactional
    public ResponseEntity<ApiResponse> deleteAddress(@PathVariable Long id, HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        Address address = addresses.findByIdAndUser_Id(id, user.getId()).orElse(null);
        if (address == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Không tìm thấy địa chỉ"));
        boolean wasDefault = address.isDefault();
        addresses.delete(address);
        if (wasDefault) {
            addresses.findByUser_IdOrderByIdDesc(user.getId()).stream().findFirst().ifPresent(next -> {
                next.setDefault(true);
                addresses.save(next);
            });
        }
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa địa chỉ"));
    }

    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse> preferences(HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        Map<String, String> result = new LinkedHashMap<>();
        for (UserPreference preference : preferences.findByUser_IdOrderByPreferenceKeyAsc(user.getId())) {
            result.put(preference.getPreferenceKey(), preference.getValue());
        }
        return ResponseEntity.ok(ApiResponse.ok("Tùy chọn tài khoản", result));
    }

    @PutMapping("/preferences/{key}")
    @Transactional
    public ResponseEntity<ApiResponse> updatePreference(@PathVariable String key,
                                                         @RequestBody Map<String, String> request,
                                                         HttpSession session) {
        User user = currentUser(session);
        if (user == null) return unauthorized();
        Set<String> allowedValues = switch (key) {
            case "locale" -> Set.of("vi", "en");
            case "currency" -> Set.of("VND", "USD");
            default -> null;
        };
        String value = request == null ? null : request.get("value");
        if (allowedValues == null || value == null || !allowedValues.contains(value)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Khóa hoặc giá trị tùy chọn không hợp lệ"));
        }

        UserPreference preference = preferences.findByUser_IdAndPreferenceKey(user.getId(), key)
                .orElseGet(() -> {
                    UserPreference created = new UserPreference();
                    created.setUser(user);
                    created.setPreferenceKey(key);
                    return created;
                });
        preference.setValue(value);
        preference.setUpdatedAt(LocalDateTime.now());
        preferences.save(preference);
        return ResponseEntity.ok(ApiResponse.ok("Đã lưu tùy chọn tài khoản", Map.of(key, value)));
    }

    private User currentUser(HttpSession session) {
        User sessionUser = (User) session.getAttribute("currentUser");
        if (sessionUser == null) return null;
        User user = users.findById(sessionUser.getId()).orElse(null);
        if (user == null) session.invalidate();
        return user;
    }

    private String validateAddress(AddressRequest request) {
        if (request == null || blank(request.receiverName()) || blank(request.phone()) || blank(request.line()) || blank(request.city())) {
            return "Vui lòng nhập người nhận, số điện thoại, địa chỉ và thành phố";
        }
        if (tooLong(request.label(), 80) || tooLong(request.receiverName(), 120) || tooLong(request.phone(), 30)
                || tooLong(request.line(), 255) || tooLong(request.ward(), 120) || tooLong(request.district(), 120)
                || tooLong(request.city(), 120)) {
            return "Thông tin địa chỉ vượt quá độ dài cho phép";
        }
        return null;
    }

    private boolean blank(String value) { return value == null || value.trim().isEmpty(); }
    private boolean tooLong(String value, int max) { return value != null && value.length() > max; }

    private void applyAddress(Address address, AddressRequest request) {
        address.setLabel(clean(request.label()));
        address.setReceiverName(clean(request.receiverName()));
        address.setPhone(clean(request.phone()));
        address.setLine(clean(request.line()));
        address.setWard(clean(request.ward()));
        address.setDistrict(clean(request.district()));
        address.setCity(clean(request.city()));
    }

    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private void clearDefault(List<Address> existing) {
        existing.forEach(address -> address.setDefault(false));
        addresses.saveAll(existing);
    }

    private ResponseEntity<ApiResponse> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Vui lòng đăng nhập để xem hồ sơ!"));
    }
}
