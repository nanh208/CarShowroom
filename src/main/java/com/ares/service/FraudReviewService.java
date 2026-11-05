package com.ares.service;

import com.ares.model.CustomerOrder;
import com.ares.model.OrderLine;
import com.ares.repository.CustomerOrderRepository;
import com.ares.repository.SandboxPaymentRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Rule-based order risk scoring, with explainable signals and a server-side review hold. */
@Service
public class FraudReviewService {
    public static final int REVIEW_AT = 60;
    public static final int WATCH_AT = 30;
    private static final Pattern VIETNAMESE_MOBILE = Pattern.compile("0[35789]\\d{8}");
    private static final Set<String> DISPOSABLE_DOMAINS = Set.of(
            "mailinator.com", "yopmail.com", "guerrillamail.com", "10minutemail.com",
            "tempmail.com", "temp-mail.org", "trashmail.com", "sharklasers.com",
            "getnada.com", "dispostable.com");

    private final CustomerOrderRepository orders;
    private final SandboxPaymentRepository payments;

    public FraudReviewService(CustomerOrderRepository orders, SandboxPaymentRepository payments) {
        this.orders = orders;
        this.payments = payments;
    }

    public record Signal(String code, int points, String reason) {}
    public record ReviewOrder(Long id, String code, String customerName, String phone,
                              String email, long total, LocalDateTime createdAt,
                              String status, int score, String riskStatus,
                              List<Signal> signals) {}

    @Transactional
    public CustomerOrder assess(CustomerOrder order) {
        if (order.getClientIp() == null) order.setClientIp(requestIp());
        List<Signal> signals = signals(order);
        int score = Math.min(100, signals.stream().mapToInt(Signal::points).sum());
        order.setFraudScore(score);
        order.setFraudReasons(signals.stream()
                .map(signal -> signal.code() + "|" + signal.points() + "|" + signal.reason())
                .reduce((left, right) -> left + "\n" + right).orElse(""));
        if (!"APPROVED".equals(order.getFraudStatus()) && !"REJECTED".equals(order.getFraudStatus())) {
            order.setFraudStatus(score >= REVIEW_AT ? "REVIEW" : score >= WATCH_AT ? "WATCH" : "NONE");
        }
        return orders.save(order);
    }

    @Transactional(readOnly = true)
    public List<ReviewOrder> getQueue() {
        List<CustomerOrder> candidates = new ArrayList<>();
        candidates.addAll(orders.findAllByStatusIgnoreCaseOrderByCreatedAtDesc("PENDING"));
        candidates.addAll(orders.findAllByStatusIgnoreCaseOrderByCreatedAtDesc("CONFIRMED"));
        return candidates.stream()
                .filter(order -> "REVIEW".equals(order.getFraudStatus()) || "WATCH".equals(order.getFraudStatus()))
                .sorted(Comparator.comparingInt(CustomerOrder::getFraudScore).reversed()
                        .thenComparing(CustomerOrder::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(FraudReviewService::toReviewOrder)
                .toList();
    }

    @Transactional
    public void approve(Long orderId, String reviewer) {
        CustomerOrder order = lockedOrder(orderId);
        requireAwaitingReview(order);
        order.setFraudStatus("APPROVED");
        order.setFraudReviewedBy(cleanReviewer(reviewer));
        order.setFraudReviewedAt(LocalDateTime.now());
        orders.save(order);
    }

    @Transactional
    public void reject(Long orderId, String reviewer, String note) {
        CustomerOrder order = lockedOrder(orderId);
        requireAwaitingReview(order);
        if (!"PENDING".equalsIgnoreCase(order.getStatus())
                && !"CONFIRMED".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể từ chối đơn chưa chuyển sang giao hàng.");
        }
        String cleanNote = note == null ? "" : note.trim();
        if (cleanNote.length() > 300) throw new IllegalArgumentException("Ghi chú từ chối tối đa 300 ký tự.");
        order.setFraudStatus("REJECTED");
        order.setFraudReviewedBy(cleanReviewer(reviewer));
        order.setFraudReviewedAt(LocalDateTime.now());
        order.setStatus("CANCELLED");
        order.setNote(appendNote(order.getNote(), "Đơn bị từ chối sau rà soát rủi ro"
                + (cleanNote.isBlank() ? "" : ": " + cleanNote)));
        orders.save(order);
    }

    public static void requireCleared(CustomerOrder order) {
        if ("REVIEW".equals(order.getFraudStatus())) {
            throw new IllegalArgumentException("Đơn " + order.getCode() + " đang chờ duyệt rủi ro ("
                    + order.getFraudScore() + " điểm). Hãy xử lý tại hàng chờ rủi ro trước khi giao.");
        }
    }

    private List<Signal> signals(CustomerOrder order) {
        List<CustomerOrder> history = orders.findAll();
        List<Signal> result = new ArrayList<>();
        String normalizedPhone = normalizePhone(order.getPhone());
        LocalDateTime createdAt = order.getCreatedAt() == null ? LocalDateTime.now() : order.getCreatedAt();

        result.add(new Signal("GUEST", 10, "Đơn được đặt không cần tài khoản"));
        if (order.getTotal() >= 15_000_000) {
            result.add(new Signal("VERY_HIGH_VALUE", 30, "Giá trị đơn từ 15 triệu đồng"));
        } else if (order.getTotal() >= 5_000_000) {
            result.add(new Signal("HIGH_VALUE", 15, "Giá trị đơn từ 5 triệu đồng"));
        }
        if ("COD".equalsIgnoreCase(order.getPaymentMethod()) && order.getTotal() >= 5_000_000) {
            result.add(new Signal("COD_HIGH", 15, "Đơn COD có giá trị lớn"));
        }
        if (!normalizedPhone.isEmpty() && !VIETNAMESE_MOBILE.matcher(normalizedPhone).matches()) {
            result.add(new Signal("BAD_PHONE", 15, "Số điện thoại không đúng định dạng di động Việt Nam"));
        }
        if (!normalizedPhone.isEmpty()) {
            List<CustomerOrder> samePhone = history.stream()
                    .filter(previous -> !sameOrder(previous, order))
                    .filter(previous -> normalizedPhone.equals(normalizePhone(previous.getPhone())))
                    .toList();
            long recentOrders = samePhone.stream()
                    .filter(previous -> isAfter(previous.getCreatedAt(), createdAt.minusHours(24))).count();
            if (recentOrders >= 2) {
                result.add(new Signal("PHONE_VELOCITY", 20,
                        "Cùng số điện thoại đã đặt nhiều đơn trong 24 giờ"));
            }
            long distinctEmails = samePhone.stream().map(CustomerOrder::getEmail)
                    .filter(email -> email != null && !email.isBlank())
                    .map(email -> email.trim().toLowerCase(Locale.ROOT)).distinct().count();
            if (distinctEmails >= 3) {
                result.add(new Signal("PHONE_SHARED", 15, "Số điện thoại được dùng với nhiều email"));
            }
            long failedOrReturned = samePhone.stream()
                    .filter(previous -> "CANCELLED".equalsIgnoreCase(previous.getStatus())
                            || "RETURNED".equalsIgnoreCase(previous.getStatus())).count();
            if (samePhone.size() >= 2 && failedOrReturned * 2 >= samePhone.size()) {
                result.add(new Signal("PHONE_ORDER_HISTORY", 20,
                        "Ít nhất một nửa đơn trước dùng cùng số điện thoại đã bị hủy hoặc hoàn"));
            }
        }
        String email = order.getEmail();
        if (email != null && email.contains("@")) {
            String domain = email.substring(email.lastIndexOf('@') + 1).trim().toLowerCase(Locale.ROOT);
            if (DISPOSABLE_DOMAINS.contains(domain)) {
                result.add(new Signal("DISPOSABLE_EMAIL", 20, "Email thuộc dịch vụ dùng một lần"));
            }
        }
        int largestLine = order.getItems().stream().mapToInt(OrderLine::getQuantity).max().orElse(0);
        if (largestLine >= 5) {
            result.add(new Signal("BULK_QUANTITY", 15, "Số lượng của một mặt hàng từ 5 trở lên"));
        }
        if (order.getShippingAddress() == null || order.getShippingAddress().trim().length() < 15) {
            result.add(new Signal("SHORT_ADDRESS", 10, "Địa chỉ giao hàng có vẻ chưa đầy đủ"));
        }
        if (order.getCustomerName() == null || order.getCustomerName().trim().split("\\s+").length < 2) {
            result.add(new Signal("SHORT_NAME", 5, "Tên người nhận có ít hơn hai từ"));
        }
        if (order.getClientIp() != null && !order.getClientIp().isBlank()) {
            long recentIpOrders = history.stream().filter(previous -> !sameOrder(previous, order))
                    .filter(previous -> order.getClientIp().equals(previous.getClientIp()))
                    .filter(previous -> isAfter(previous.getCreatedAt(), createdAt.minusHours(1))).count();
            if (recentIpOrders >= 3) {
                result.add(new Signal("IP_VELOCITY", 20, "Cùng địa chỉ IP đã đặt nhiều đơn trong một giờ"));
            }
        }
        long failedPayments = payments.findByOrderCodeAndStatus(order.getCode(), "FAILED").size();
        if (failedPayments >= 3) {
            result.add(new Signal("PAYMENT_FAILURES", 15, "Đơn hàng có từ ba lần thanh toán thất bại"));
        }
        return List.copyOf(result);
    }

    private CustomerOrder lockedOrder(Long orderId) {
        return orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
    }

    private static void requireAwaitingReview(CustomerOrder order) {
        if (!"REVIEW".equals(order.getFraudStatus()) && !"WATCH".equals(order.getFraudStatus())) {
            throw new IllegalArgumentException("Đơn không còn trong hàng chờ rà soát.");
        }
    }

    private static ReviewOrder toReviewOrder(CustomerOrder order) {
        List<Signal> signals = Arrays.stream(order.getFraudReasons() == null
                        ? new String[0] : order.getFraudReasons().split("\\R"))
                .map(value -> value.split("\\|", 3))
                .filter(parts -> parts.length == 3)
                .map(parts -> new Signal(parts[0], parsePoints(parts[1]), parts[2]))
                .toList();
        return new ReviewOrder(order.getId(), order.getCode(), order.getCustomerName(), order.getPhone(),
                order.getEmail(), order.getTotal(), order.getCreatedAt(), order.getStatus(),
                order.getFraudScore(), order.getFraudStatus(), signals);
    }

    private static int parsePoints(String value) {
        try { return Integer.parseInt(value); } catch (NumberFormatException ex) { return 0; }
    }

    private static boolean sameOrder(CustomerOrder left, CustomerOrder right) {
        return left.getId() != null && left.getId().equals(right.getId());
    }

    private static boolean isAfter(LocalDateTime value, LocalDateTime threshold) {
        return value != null && value.isAfter(threshold);
    }

    private static String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("\\D", "");
        return digits.startsWith("84") ? "0" + digits.substring(2) : digits;
    }

    private static String requestIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes == null ? null : attributes.getRequest().getRemoteAddr();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String cleanReviewer(String reviewer) {
        if (reviewer == null || reviewer.isBlank()) return "staff";
        String value = reviewer.trim();
        return value.length() > 100 ? value.substring(0, 100) : value;
    }

    private static String appendNote(String current, String addition) {
        String result = current == null || current.isBlank() ? addition : current.trim() + "\n" + addition;
        return result.length() > 500 ? result.substring(0, 500) : result;
    }
}
