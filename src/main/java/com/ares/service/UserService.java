package com.ares.service;

import com.ares.dto.ApiResponse;
import com.ares.dto.AuthDtos.RegisterRequest;
import com.ares.dto.AuthDtos.ResetPasswordRequest;
import com.ares.model.User;
import com.ares.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final VerificationService verificationService;

    @Autowired
    public UserService(UserRepository userRepository, VerificationService verificationService) {
        this.userRepository = userRepository;
        this.verificationService = verificationService;
    }

    /**
     * User registration implementation matching Image 3 and Activity Diagram.
     */
    @Transactional
    public ApiResponse register(RegisterRequest req) {
        if (req.getUsername() == null || req.getUsername().trim().isEmpty()) {
            return ApiResponse.error("Tên người dùng không được để trống!");
        }

        if (req.getPassword() == null || req.getPassword().length() < 4) {
            return ApiResponse.error("Mật khẩu phải có ít nhất 4 ký tự!");
        }

        if (!req.getPassword().equals(req.getConfirmPassword())) {
            return ApiResponse.error("Mật khẩu nhập lại không trùng khớp!");
        }

        if (userRepository.existsByUsername(req.getUsername().trim())) {
            return ApiResponse.error("Tên người dùng này đã được sử dụng!");
        }

        String target = req.getEmailOrPhone() != null ? req.getEmailOrPhone().trim() : "";
        if (target.isEmpty()) {
            return ApiResponse.error("Email hoặc số điện thoại không được để trống!");
        }

        // Verify verification code
        boolean isCodeValid = verificationService.verifyCode(target, req.getVerificationCode());
        if (!isCodeValid) {
            return ApiResponse.error("Mã xác nhận không đúng hoặc đã hết hạn!");
        }

        User user = new User();
        user.setUsername(req.getUsername().trim());
        user.setPassword(req.getPassword()); // Note: In production BCryptPasswordEncoder should be used
        user.setBirthDay(req.getBirthDay());
        user.setBirthMonth(req.getBirthMonth());
        user.setGender(req.getGender() != null ? req.getGender() : "Nam");

        if (target.contains("@")) {
            user.setEmail(target.toLowerCase());
            user.setVerifiedType("GOOGLE"); // Marked as verified with Gmail (as a base)
        } else {
            user.setPhone(target);
            user.setVerifiedType("PHONE");  // Marked as verified with Phone (as a base)
        }

        userRepository.save(user);
        return ApiResponse.ok("Đăng ký tài khoản thành công!");
    }

    /**
     * Authenticates user with username, email, or phone number and password.
     */
    public Optional<User> authenticate(String usernameOrEmailOrPhone, String password) {
        if (usernameOrEmailOrPhone == null || password == null) return Optional.empty();

        String trimmed = usernameOrEmailOrPhone.trim();
        Optional<User> userOpt = userRepository.findByUsername(trimmed);
        if (userOpt.isEmpty() && trimmed.contains("@")) {
            userOpt = userRepository.findByEmail(trimmed.toLowerCase());
        }
        if (userOpt.isEmpty()) {
            userOpt = userRepository.findByPhone(trimmed);
        }

        if (userOpt.isPresent() && userOpt.get().getPassword().equals(password)) {
            return userOpt;
        }

        return Optional.empty();
    }

    /**
     * Authenticates or creates user via Phone OTP login matching car-copy behavior.
     */
    @Transactional
    public User authenticateOrRegisterByPhoneOtp(String phone) {
        String cleanPhone = phone.trim();
        return userRepository.findByPhone(cleanPhone).orElseGet(() -> {
            User newUser = new User();
            newUser.setPhone(cleanPhone);
            String suffix = cleanPhone.length() >= 4 ? cleanPhone.substring(cleanPhone.length() - 4) : cleanPhone;
            String generatedUsername = "user_" + suffix + "_" + (System.currentTimeMillis() % 1000);
            if (userRepository.existsByUsername(generatedUsername)) {
                generatedUsername += "_" + (System.currentTimeMillis() % 100);
            }
            newUser.setUsername(generatedUsername);
            newUser.setPassword("PHONE_AUTH_" + System.currentTimeMillis());
            newUser.setVerifiedType("PHONE");
            return userRepository.save(newUser);
        });
    }

    /**
     * Resets password matching Image 4 & 5 and Activity Diagram.
     */
    @Transactional
    public ApiResponse resetPassword(ResetPasswordRequest req) {
        if (req.getTarget() == null || req.getTarget().trim().isEmpty()) {
            return ApiResponse.error("Target email hoặc số điện thoại không hợp lệ!");
        }

        if (req.getNewPassword() == null || req.getNewPassword().length() < 4) {
            return ApiResponse.error("Mật khẩu mới phải có ít nhất 4 ký tự!");
        }

        String cleanTarget = req.getTarget().trim();
        boolean verified = verificationService.isTargetVerified(cleanTarget) 
                        || verificationService.verifyCode(cleanTarget, req.getCode());

        if (!verified) {
            return ApiResponse.error("Mã xác minh không hợp lệ hoặc phiên đổi mật khẩu đã hết hạn!");
        }

        Optional<User> userOpt;
        if (cleanTarget.contains("@")) {
            userOpt = userRepository.findByEmail(cleanTarget.toLowerCase());
        } else {
            userOpt = userRepository.findByPhone(cleanTarget);
        }

        if (userOpt.isEmpty()) {
            // For convenience, search by username too
            userOpt = userRepository.findByUsername(cleanTarget);
        }

        if (userOpt.isEmpty()) {
            return ApiResponse.error("Không tìm thấy tài khoản với thông tin đã cung cấp!");
        }

        User user = userOpt.get();
        user.setPassword(req.getNewPassword());
        userRepository.save(user);

        return ApiResponse.ok("Đặt lại mật khẩu thành công!");
    }

    /**
     * OAuth Google simulation: System creates or links account to Gmail,
     * marks as verified with Gmail (as a base) as specified in the Activity Diagram.
     */
    @Transactional
    public User processGoogleOAuth(String email, String displayName) {
        String cleanEmail = email.toLowerCase().trim();
        return userRepository.findByEmail(cleanEmail).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(cleanEmail);
            String generatedUsername = cleanEmail.split("@")[0];
            if (userRepository.existsByUsername(generatedUsername)) {
                generatedUsername += "_" + System.currentTimeMillis() % 1000;
            }
            newUser.setUsername(generatedUsername);
            newUser.setPassword("OAUTH_" + System.currentTimeMillis());
            newUser.setVerifiedType("GOOGLE");
            return userRepository.save(newUser);
        });
    }

    /**
     * OAuth Facebook simulation: System creates or links account to Facebook identity.
     */
    @Transactional
    public User processFacebookOAuth(String emailOrId, String displayName) {
        String identifier = emailOrId.toLowerCase().trim();
        Optional<User> userOpt = identifier.contains("@") 
                ? userRepository.findByEmail(identifier) 
                : userRepository.findByUsername("fb_" + identifier);
        return userOpt.orElseGet(() -> {
            User newUser = new User();
            if (identifier.contains("@")) {
                newUser.setEmail(identifier);
            }
            String baseUsername = displayName != null && !displayName.isBlank()
                    ? displayName.toLowerCase().replaceAll("[^a-z0-9]", "")
                    : "fb_user";
            if (baseUsername.isBlank()) baseUsername = "fb_user";
            String gen = baseUsername;
            if (userRepository.existsByUsername(gen)) {
                gen += "_" + (System.currentTimeMillis() % 1000);
            }
            newUser.setUsername(gen);
            newUser.setPassword("FB_OAUTH_" + System.currentTimeMillis());
            newUser.setVerifiedType("FACEBOOK");
            return userRepository.save(newUser);
        });
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}
