package com.ares.service;

import com.ares.dto.PaymentDtos.CompletePaymentRequest;
import com.ares.dto.PaymentDtos.CreatePaymentRequest;
import com.ares.dto.PaymentDtos.PaymentResponse;
import com.ares.model.CustomerOrder;
import com.ares.model.SandboxPayment;
import com.ares.repository.CustomerOrderRepository;
import com.ares.repository.SandboxPaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class SandboxPaymentService {
    private final CustomerOrderRepository orders;
    private final SandboxPaymentRepository payments;

    public SandboxPaymentService(CustomerOrderRepository orders, SandboxPaymentRepository payments) {
        this.orders = orders;
        this.payments = payments;
    }

    @Transactional
    public PaymentResponse create(String orderCode, CreatePaymentRequest request) {
        CustomerOrder order = authorizedOrder(orderCode, request == null ? null : request.orderToken());
        if ("PAID".equals(order.getPaymentStatus())) throw new IllegalArgumentException("Đơn hàng đã được thanh toán");
        String provider = request.provider() == null ? "" : request.provider().trim().toUpperCase(Locale.ROOT);
        if (!List.of("VNPAY_SANDBOX", "MOMO_SANDBOX").contains(provider) || !provider.equals(order.getPaymentMethod())) {
            throw new IllegalArgumentException("Cổng mô phỏng không khớp với phương thức của đơn hàng");
        }
        LocalDateTime now = LocalDateTime.now();
        for (SandboxPayment old : payments.findByOrderCodeAndStatus(orderCode, "PENDING")) {
            old.setStatus("CANCELLED");
            old.setResponseCode("REPLACED");
            old.setCompletedAt(now);
            payments.save(old);
        }
        SandboxPayment payment = new SandboxPayment();
        payment.setTransactionRef("SIM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT));
        payment.setOrderCode(order.getCode());
        payment.setProvider(provider);
        payment.setAmount(order.getTotal());
        payment.setCreatedAt(now);
        payment.setExpiresAt(now.plusMinutes(15));
        return response(payments.save(payment), order);
    }

    @Transactional
    public PaymentResponse complete(String transactionRef, CompletePaymentRequest request) {
        if (request == null) throw new IllegalArgumentException("Thiếu kết quả mô phỏng");
        SandboxPayment payment = payments.findByTransactionRef(transactionRef)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy giao dịch mô phỏng"));
        CustomerOrder order = authorizedOrder(payment.getOrderCode(), request.orderToken());
        if (payment.getAmount() != order.getTotal()) throw new IllegalArgumentException("Số tiền giao dịch không khớp đơn hàng");
        String code = request.responseCode() == null ? "" : request.responseCode().trim();
        if (!List.of("00", "24", "51").contains(code)) throw new IllegalArgumentException("Mã kết quả mô phỏng không hợp lệ");
        if ("SUCCESS".equals(payment.getStatus()) && "00".equals(code)) return response(payment, order);
        if (!"PENDING".equals(payment.getStatus())) throw new IllegalArgumentException("Giao dịch đã kết thúc, không thể thay đổi kết quả");
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(payment.getExpiresAt())) {
            payment.setStatus("EXPIRED");
            payment.setCompletedAt(now);
            payment.setResponseCode("EXPIRED");
            return response(payments.save(payment), order);
        }
        if ("PAID".equals(order.getPaymentStatus())) throw new IllegalArgumentException("Đơn hàng đã được thanh toán");
        payment.setResponseCode(code);
        payment.setCompletedAt(now);
        if ("00".equals(code)) {
            payment.setStatus("SUCCESS");
            order.setPaymentStatus("PAID");
            order.setPaidAt(now);
            orders.save(order);
        } else if ("24".equals(code)) {
            payment.setStatus("CANCELLED");
        } else {
            payment.setStatus("FAILED");
        }
        return response(payments.save(payment), order);
    }

    private CustomerOrder authorizedOrder(String orderCode, String token) {
        CustomerOrder order = orders.findByCode(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));
        if (token == null || token.isBlank() || order.getPaymentTokenHash() == null
                || !MessageDigest.isEqual(order.getPaymentTokenHash().getBytes(StandardCharsets.US_ASCII), sha256(token).getBytes(StandardCharsets.US_ASCII))) {
            throw new IllegalArgumentException("Mã xác nhận thanh toán không hợp lệ");
        }
        return order;
    }

    private String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Không thể xác thực mã thanh toán", ex);
        }
    }

    private PaymentResponse response(SandboxPayment payment, CustomerOrder order) {
        return new PaymentResponse(payment.getTransactionRef(), payment.getOrderCode(), payment.getProvider(),
                payment.getAmount(), payment.getStatus(), payment.getExpiresAt(), order.getPaymentStatus());
    }
}
