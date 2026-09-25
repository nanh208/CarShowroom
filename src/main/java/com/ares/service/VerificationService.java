package com.ares.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VerificationService {

    private static final Logger log = LoggerFactory.getLogger(VerificationService.class);
    private static final SecureRandom random = new SecureRandom();
    private static final long CODE_VALIDITY_SECONDS = 300; // 5 phút hiệu lực mã
    private static final long COOLDOWN_SECONDS = 2;         // 2s chống double-click; load lại trang là gửi được ngay
    private static final int MAX_REQUESTS_IN_WINDOW = 30;   // Thoải mái kiểm thử
    private static final long WINDOW_SECONDS = 600;         // Cửa sổ 10 phút

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailSenderUsername;

    private static class VerificationEntry {
        final String code;
        final Instant expiresAt;

        VerificationEntry(String code, Instant expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private static class RateLimitEntry {
        Instant lastSentAt;
        int count;
        Instant windowStart;

        RateLimitEntry(Instant now) {
            this.lastSentAt = now;
            this.count = 1;
            this.windowStart = now;
        }
    }

    // In-memory code store: target -> VerificationEntry
    private final Map<String, VerificationEntry> codeStore = new ConcurrentHashMap<>();
    // Verified tokens: target -> verified timestamp
    private final Map<String, Instant> verifiedStore = new ConcurrentHashMap<>();
    // Rate limit store: target -> RateLimitEntry
    private final Map<String, RateLimitEntry> rateLimitStore = new ConcurrentHashMap<>();

    /**
     * Generate and dispatch a 6-digit verification code with RATE LIMITING.
     * Prevents spamming and multiple clicks.
     */
    public synchronized String generateAndSendCode(String target, boolean isEmail) {
        if (target == null || target.trim().isEmpty()) {
            throw new IllegalArgumentException("Target cannot be empty");
        }

        String cleanTarget = target.trim().toLowerCase();
        Instant now = Instant.now();

        // 1. Kiểm tra Rate Limiting (Giới hạn tần suất gửi)
        RateLimitEntry rateEntry = rateLimitStore.get(cleanTarget);
        if (rateEntry != null) {
            // Kiểm tra cửa sổ 10 phút
            long windowElapsed = Duration.between(rateEntry.windowStart, now).getSeconds();
            if (windowElapsed > WINDOW_SECONDS) {
                // Reset window
                rateEntry.windowStart = now;
                rateEntry.count = 0;
            }

            // Kiểm tra số lần gửi tối đa trong cửa sổ 10 phút
            if (rateEntry.count >= MAX_REQUESTS_IN_WINDOW) {
                long waitTime = WINDOW_SECONDS - windowElapsed;
                long waitMinutes = Math.max(1, waitTime / 60);
                throw new IllegalStateException("Bạn đã yêu cầu gửi mã quá 5 lần. Vui lòng đợi " + waitMinutes + " phút nữa trước khi thử lại để bảo vệ tài khoản!");
            }

            // Kiểm tra thời gian chờ tối thiểu giữa 2 lần bấm (Cooldown 60s)
            long elapsedSinceLast = Duration.between(rateEntry.lastSentAt, now).getSeconds();
            if (elapsedSinceLast < COOLDOWN_SECONDS) {
                long remainingWait = COOLDOWN_SECONDS - elapsedSinceLast;
                throw new IllegalStateException("Bạn bấm gửi mã quá nhanh! Vui lòng đợi " + remainingWait + " giây nữa để gửi lại.");
            }

            // Cập nhật lượt gửi
            rateEntry.lastSentAt = now;
            rateEntry.count++;
        } else {
            rateLimitStore.put(cleanTarget, new RateLimitEntry(now));
        }

        // 2. Sinh mã xác thực ngẫu nhiên 6 chữ số
        String code = String.format("%06d", random.nextInt(1_000_000));
        Instant expiresAt = now.plusSeconds(CODE_VALIDITY_SECONDS);
        codeStore.put(cleanTarget, new VerificationEntry(code, expiresAt));

        // 3. Gửi mã qua Email hoặc SMS
        if (isEmail || cleanTarget.contains("@")) {
            log.info("[GMAIL PROVIDER] Khởi tạo mã xác nhận Gmail tới {}: [{}] (Hết hạn sau 5 phút)", cleanTarget, code);
            sendRealEmailIfConfigured(cleanTarget, code);
        } else {
            log.info("[SMS PROVIDER] Gửi mã OTP SMS tới {}: [{}] <Chế độ an toàn: Hạn chế test tránh mất tiền>", cleanTarget, code);
        }

        return code;
    }

    /**
     * Sends a real email via Gmail SMTP if configured.
     */
    private void sendRealEmailIfConfigured(String toEmail, String code) {
        if (mailSender != null && mailSenderUsername != null 
                && !mailSenderUsername.trim().isEmpty() 
                && !mailSenderUsername.contains("your-email")) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailSenderUsername);
                message.setTo(toEmail);
                message.setSubject("[Ares Store] Mã xác thực của bạn: " + code);
                message.setText(
                    "Xin chào,\n\n" +
                    "Mã xác thực tài khoản Ares Store của bạn là: " + code + "\n\n" +
                    "Mã này có hiệu lực trong vòng 5 phút. Vui lòng không chia sẻ mã này cho bất kỳ ai để bảo vệ an toàn cho tài khoản.\n\n" +
                    "Trân trọng,\n" +
                    "Ares Store Car Showroom"
                );
                mailSender.send(message);
                log.info("===> [EMAIL THÀNH CÔNG] Đã gửi mã xác nhận {} đến hộp thư Gmail: {}", code, toEmail);
            } catch (Exception ex) {
                log.error("===> [EMAIL LỖI] Không thể gửi email qua SMTP: {}. (Mã OTP vẫn có thể dùng trên giao diện)", ex.getMessage());
            }
        } else {
            log.info("===> [EMAIL CHƯA CẤU HÌNH SMTP] Hệ thống dùng chế độ Safe Dev: Mã OTP hiển thị qua Toast và Log: {}", code);
        }
    }

    /**
     * Validates the provided code for the target.
     */
    public boolean verifyCode(String target, String inputCode) {
        if (target == null || inputCode == null) return false;

        String cleanTarget = target.trim().toLowerCase();
        VerificationEntry entry = codeStore.get(cleanTarget);

        if (entry == null) {
            log.warn("Không tìm thấy mã xác nhận cho target: {}", cleanTarget);
            return false;
        }

        if (entry.isExpired()) {
            codeStore.remove(cleanTarget);
            log.warn("Mã xác nhận cho {} đã hết hạn", cleanTarget);
            return false;
        }

        if (entry.code.equals(inputCode.trim())) {
            codeStore.remove(cleanTarget);
            verifiedStore.put(cleanTarget, Instant.now().plusSeconds(600)); // verified valid for 10 mins
            log.info("Xác thực mã thành công cho {}", cleanTarget);
            return true;
        }

        log.warn("Mã xác nhận nhập vào không đúng cho {}. Nhập: {}, Đúng: {}", cleanTarget, inputCode, entry.code);
        return false;
    }

    /**
     * Checks if a target is currently verified.
     */
    public boolean isTargetVerified(String target) {
        if (target == null) return false;
        String cleanTarget = target.trim().toLowerCase();
        Instant validUntil = verifiedStore.get(cleanTarget);
        if (validUntil == null) return false;
        if (Instant.now().isAfter(validUntil)) {
            verifiedStore.remove(cleanTarget);
            return false;
        }
        return true;
    }
}
