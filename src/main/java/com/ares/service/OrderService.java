package com.ares.service;

import com.ares.dto.OrderDtos.CreateOrderRequest;
import com.ares.dto.OrderDtos.CreateOrderResponse;
import com.ares.dto.OrderDtos.OrderLineRequest;
import com.ares.dto.OrderDtos.OrderLineResponse;
import com.ares.model.Car;
import com.ares.model.CustomerOrder;
import com.ares.model.OrderLine;
import com.ares.repository.CarRepository;
import com.ares.repository.CustomerOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

@Service
public class OrderService {

    private static final int MAX_ITEMS = 30;
    private static final int MAX_QUANTITY_PER_CAR = 99;
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9][0-9 .-]{7,18}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CarRepository cars;
    private final CustomerOrderRepository orders;

    public OrderService(CarRepository cars, CustomerOrderRepository orders) {
        this.cars = cars;
        this.orders = orders;
    }

    @Transactional
    public CreateOrderResponse place(CreateOrderRequest request) {
        validateCustomer(request);
        Map<String, Integer> quantities = aggregateItems(request.items());

        CustomerOrder order = new CustomerOrder();
        order.setCode(createCode());
        order.setCustomerName(request.customerName().trim());
        order.setPhone(request.phone().trim());
        order.setEmail(blankToNull(request.email()));
        order.setShippingAddress(request.shippingAddress().trim());
        order.setNote(blankToNull(request.note()));
        order.setStatus("PENDING");
        String paymentMethod = request.paymentMethod() == null ? "COD" : request.paymentMethod().trim().toUpperCase(Locale.ROOT);
        if (!List.of("COD", "VNPAY_SANDBOX", "MOMO_SANDBOX").contains(paymentMethod)) {
            throw new IllegalArgumentException("Phương thức thanh toán không hợp lệ");
        }
        order.setPaymentMethod(paymentMethod);
        String paymentToken = null;
        if (!"COD".equals(paymentMethod)) {
            byte[] tokenBytes = new byte[32];
            RANDOM.nextBytes(tokenBytes);
            paymentToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
            order.setPaymentTokenHash(sha256(paymentToken));
        }
        order.setCreatedAt(LocalDateTime.now());

        long total = 0;
        int totalQuantity = 0;
        List<OrderLineResponse> responseLines = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : quantities.entrySet()) {
            List<Car> matches = cars.findAllByCodeIgnoreCase(entry.getKey());
            if (matches.isEmpty()) throw new IllegalArgumentException("Không tìm thấy sản phẩm " + entry.getKey());
            if (matches.size() > 1) throw new IllegalArgumentException("Mã sản phẩm " + entry.getKey() + " chưa duy nhất trong dữ liệu");
            Car car = matches.get(0);
            if (!Boolean.TRUE.equals(car.getInStock())) {
                throw new IllegalArgumentException("Sản phẩm \"" + car.getName() + "\" hiện đã hết hàng");
            }
            if (car.getPrice() == null || car.getPrice() <= 0) {
                throw new IllegalArgumentException("Giá của sản phẩm \"" + car.getName() + "\" không hợp lệ");
            }

            int quantity = entry.getValue();
            long subtotal = Math.multiplyExact(car.getPrice(), quantity);
            total = Math.addExact(total, subtotal);
            totalQuantity = Math.addExact(totalQuantity, quantity);
            order.addItem(new OrderLine(car.getId(), car.getCode(), car.getName(), car.getImage(), quantity, car.getPrice(), subtotal));
            responseLines.add(new OrderLineResponse(car.getCode(), car.getId(), car.getName(), car.getImage(), quantity, car.getPrice(), subtotal));
        }

        order.setTotal(total);
        order.setTotalQuantity(totalQuantity);
        CustomerOrder saved = orders.save(order);
        return new CreateOrderResponse(saved.getCode(), saved.getStatus(), saved.getPaymentMethod(), saved.getPaymentStatus(), paymentToken, saved.getTotal(),
                saved.getTotalQuantity(), saved.getCreatedAt(), List.copyOf(responseLines));
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Không thể tạo mã thanh toán an toàn", ex);
        }
    }

    private void validateCustomer(CreateOrderRequest request) {
        if (request == null) throw new IllegalArgumentException("Thiếu thông tin đặt hàng");
        if (isBlank(request.customerName()) || request.customerName().trim().length() < 2 || request.customerName().trim().length() > 100) {
            throw new IllegalArgumentException("Họ tên cần có từ 2 đến 100 ký tự");
        }
        if (isBlank(request.phone()) || !PHONE.matcher(request.phone().trim()).matches()) {
            throw new IllegalArgumentException("Vui lòng nhập số điện thoại hợp lệ");
        }
        if (!isBlank(request.email()) && (request.email().trim().length() > 180 || !EMAIL.matcher(request.email().trim()).matches())) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }
        if (isBlank(request.shippingAddress()) || request.shippingAddress().trim().length() < 8 || request.shippingAddress().trim().length() > 300) {
            throw new IllegalArgumentException("Địa chỉ nhận hàng cần có từ 8 đến 300 ký tự");
        }
        if (request.note() != null && request.note().length() > 500) {
            throw new IllegalArgumentException("Ghi chú không được vượt quá 500 ký tự");
        }
    }

    private Map<String, Integer> aggregateItems(List<OrderLineRequest> items) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("Giỏ hàng đang trống");
        if (items.size() > MAX_ITEMS) throw new IllegalArgumentException("Đơn hàng chỉ nhận tối đa 30 loại sản phẩm");

        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (OrderLineRequest item : items) {
            if (item == null || isBlank(item.productCode()) || item.productCode().trim().length() > 80
                    || item.quantity() == null || item.quantity() < 1) {
                throw new IllegalArgumentException("Sản phẩm hoặc số lượng đặt hàng không hợp lệ");
            }
            String productCode = item.productCode().trim().toUpperCase(Locale.ROOT);
            int quantity;
            try {
                quantity = Math.addExact(quantities.getOrDefault(productCode, 0), item.quantity());
            } catch (ArithmeticException ex) {
                throw new IllegalArgumentException("Số lượng đặt hàng quá lớn");
            }
            if (quantity > MAX_QUANTITY_PER_CAR) {
                throw new IllegalArgumentException("Mỗi loại sản phẩm chỉ được đặt tối đa 99 món");
            }
            quantities.put(productCode, quantity);
        }
        return quantities;
    }

    private String createCode() {
        String day = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ARES-" + day + "-" + suffix;
    }

    private static String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
