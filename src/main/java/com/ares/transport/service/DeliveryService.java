package com.ares.transport.service;

import com.ares.transport.model.Delivery;
import com.ares.transport.model.DeliveryEvent;
import com.ares.transport.model.DeliveryStatus;
import com.ares.transport.repository.DeliveryEventRepository;
import com.ares.transport.repository.DeliveryRepository;
import com.ares.model.CustomerOrder;
import com.ares.model.OrderLine;
import com.ares.repository.CustomerOrderRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryService {
    private final DeliveryRepository deliveries;
    private final DeliveryEventRepository events;
    private final CustomerOrderRepository orders;

    public DeliveryService(DeliveryRepository deliveries, DeliveryEventRepository events,
                           CustomerOrderRepository orders) {
        this.deliveries = deliveries;
        this.events = events;
        this.orders = orders;
    }

    public record PendingLine(String productCode, String carName, int quantity) {}
    public record PendingOrder(Long id, String code, String customerName, String phone,
                               String shippingAddress, String paymentMethod, String paymentStatus,
                               long total, int totalQuantity, LocalDateTime createdAt,
                               List<PendingLine> items) {}
    public record TrackingEvent(DeliveryStatus status, String description, LocalDateTime occurredAt) {}
    public record TrackingDetails(String trackingNumber, String orderCode, String carrier,
                                  DeliveryStatus status, List<TrackingEvent> events) {}

    @Transactional(readOnly = true)
    public List<Delivery> getDeliveries() { return deliveries.findAllByOrderByCreatedAtDesc(); }

    @Transactional(readOnly = true)
    public Optional<Delivery> findDelivery(Long id) { return deliveries.findById(id); }

    @Transactional(readOnly = true)
    public List<PendingOrder> getPendingOrders() {
        Set<Long> linkedOrderIds = new HashSet<>(deliveries.findLinkedOrderIds());
        return orders.findAllByStatusIgnoreCaseOrderByCreatedAtDesc("PENDING").stream()
                .filter(order -> !linkedOrderIds.contains(order.getId()))
                .filter(DeliveryService::isPaymentReady)
                .map(DeliveryService::toPendingOrder)
                .toList();
    }

    @Transactional
    public void create(String orderCode, String recipientName, String phone, String address,
                       String city, String carrier) {
        String cleanCarrier = cleanCarrier(carrier);
        Delivery delivery = deliveries.save(new Delivery(required(orderCode, "Mã đơn"),
                required(recipientName, "Người nhận"), required(phone, "Số điện thoại"),
                required(address, "Địa chỉ"), required(city, "Tỉnh/thành phố"), cleanCarrier, newTrackingNumber()));
        addEvent(delivery, DeliveryStatus.READY);
    }

    @Transactional
    public void createFromOrder(Long orderId, String city, String carrier) {
        CustomerOrder order = orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));
        if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
            throw new IllegalArgumentException("Chỉ có thể tạo vận đơn cho đơn đang chờ xử lý.");
        }
        if (!isPaymentReady(order)) {
            throw new IllegalArgumentException("Đơn thanh toán trực tuyến cần được thanh toán thành công trước khi giao.");
        }
        if (order.getItems().isEmpty()) throw new IllegalArgumentException("Đơn hàng không có sản phẩm.");
        if (deliveries.existsByOrderId(orderId)) throw new IllegalArgumentException("Đơn hàng đã có vận đơn.");

        Delivery delivery = deliveries.save(new Delivery(order.getId(), order.getCode(), order.getCustomerName(),
                order.getPhone(), order.getShippingAddress(), required(city, "Tỉnh/thành phố"),
                cleanCarrier(carrier), newTrackingNumber()));
        addEvent(delivery, DeliveryStatus.READY);
    }

    @Transactional
    public void updateStatus(Long id, DeliveryStatus next) {
        Delivery delivery = deliveries.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vận đơn."));
        DeliveryStatus current = delivery.getStatus();
        boolean allowed = switch (current) {
            case READY -> next == DeliveryStatus.IN_TRANSIT || next == DeliveryStatus.RETURNED;
            case IN_TRANSIT -> next == DeliveryStatus.OUT_FOR_DELIVERY || next == DeliveryStatus.RETURNED;
            case OUT_FOR_DELIVERY -> next == DeliveryStatus.DELIVERED || next == DeliveryStatus.RETURNED;
            case DELIVERED, RETURNED -> false;
        };
        if (!allowed) throw new IllegalArgumentException("Không thể chuyển trạng thái " + label(current) + " sang " + label(next) + ".");

        CustomerOrder order = delivery.getOrderId() == null ? null : orders.findByIdForUpdate(delivery.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng liên kết với vận đơn."));
        if (order != null) syncOrderStatus(order, next);
        delivery.setStatus(next);
        delivery.markUpdated();
        addEvent(delivery, next);
    }

    @Transactional(readOnly = true)
    public Optional<TrackingDetails> findTracking(String trackingNumber) {
        if (trackingNumber == null || trackingNumber.isBlank()) return Optional.empty();
        return deliveries.findByTrackingNumberIgnoreCase(trackingNumber.trim())
                .map(delivery -> new TrackingDetails(delivery.getTrackingNumber(), delivery.getOrderCode(),
                        delivery.getCarrier(), delivery.getStatus(),
                        events.findAllByDeliveryIdOrderByOccurredAtAsc(delivery.getId()).stream()
                                .map(event -> new TrackingEvent(event.getStatus(), event.getDescription(), event.getOccurredAt()))
                                .toList()));
    }

    private static String description(DeliveryStatus status, String carrier) {
        return switch (status) {
            case READY -> "Đã tạo vận đơn, chờ bàn giao cho đơn vị vận chuyển.";
            case IN_TRANSIT -> "Đã bàn giao vận đơn cho " + carrier + ".";
            case OUT_FOR_DELIVERY -> "Vận đơn đang được giao tới người nhận.";
            case DELIVERED -> "Giao hàng thành công.";
            case RETURNED -> "Vận đơn được ghi nhận hoàn hàng.";
        };
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " không được để trống.");
        return value.trim();
    }

    private static String label(DeliveryStatus status) {
        return status.getLabel();
    }

    private static PendingOrder toPendingOrder(CustomerOrder order) {
        List<PendingLine> lines = order.getItems().stream()
                .map(item -> new PendingLine(item.getProductCode(), item.getCarName(), item.getQuantity()))
                .toList();
        return new PendingOrder(order.getId(), order.getCode(), order.getCustomerName(), order.getPhone(),
                order.getShippingAddress(), order.getPaymentMethod(), order.getPaymentStatus(), order.getTotal(),
                order.getTotalQuantity(), order.getCreatedAt(), lines);
    }

    private static boolean isPaymentReady(CustomerOrder order) {
        return "COD".equalsIgnoreCase(order.getPaymentMethod())
                || "PAID".equalsIgnoreCase(order.getPaymentStatus());
    }

    private static void syncOrderStatus(CustomerOrder order, DeliveryStatus next) {
        switch (next) {
            case IN_TRANSIT -> {
                if (!"PENDING".equalsIgnoreCase(order.getStatus())) {
                    throw new IllegalArgumentException("Đơn hàng không còn ở trạng thái chờ giao.");
                }
                if (!isPaymentReady(order)) {
                    throw new IllegalArgumentException("Chưa thể bàn giao đơn chưa thanh toán.");
                }
                order.setStatus("SHIPPING");
            }
            case DELIVERED -> {
                if (!"SHIPPING".equalsIgnoreCase(order.getStatus())) {
                    throw new IllegalArgumentException("Đơn hàng chưa được bàn giao vận chuyển.");
                }
                if ("COD".equalsIgnoreCase(order.getPaymentMethod())
                        && !"PAID".equalsIgnoreCase(order.getPaymentStatus())) {
                    order.setPaymentStatus("PAID");
                    order.setPaidAt(LocalDateTime.now());
                }
                order.setStatus("COMPLETED");
            }
            case RETURNED -> order.setStatus("RETURNED");
            case READY, OUT_FOR_DELIVERY -> { }
        }
    }

    private void addEvent(Delivery delivery, DeliveryStatus status) {
        events.save(new DeliveryEvent(delivery, status, description(status, delivery.getCarrier())));
    }

    private String cleanCarrier(String carrier) {
        String clean = required(carrier, "Đơn vị vận chuyển").toUpperCase(Locale.ROOT);
        if (!List.of("GHN", "GHTK", "OTHER").contains(clean)) {
            throw new IllegalArgumentException("Chọn GHN, GHTK hoặc đơn vị khác.");
        }
        return clean;
    }

    private String newTrackingNumber() {
        String tracking;
        do {
            tracking = "ARES-" + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 12).toUpperCase(Locale.ROOT);
        } while (deliveries.existsByTrackingNumber(tracking));
        return tracking;
    }
}
