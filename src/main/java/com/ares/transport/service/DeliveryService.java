package com.ares.transport.service;

import com.ares.transport.model.Delivery;
import com.ares.transport.model.DeliveryEvent;
import com.ares.transport.model.DeliveryStatus;
import com.ares.transport.repository.DeliveryEventRepository;
import com.ares.transport.repository.DeliveryRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryService {
    private final DeliveryRepository deliveries;
    private final DeliveryEventRepository events;

    public DeliveryService(DeliveryRepository deliveries, DeliveryEventRepository events) {
        this.deliveries = deliveries;
        this.events = events;
    }

    public record TrackingEvent(DeliveryStatus status, String description, LocalDateTime occurredAt) {}
    public record TrackingDetails(String trackingNumber, String orderCode, String carrier,
                                  DeliveryStatus status, List<TrackingEvent> events) {}

    @Transactional(readOnly = true)
    public List<Delivery> getDeliveries() { return deliveries.findAllByOrderByCreatedAtDesc(); }

    @Transactional
    public void create(String orderCode, String recipientName, String phone, String address,
                       String city, String carrier) {
        String cleanCarrier = required(carrier, "Đơn vị vận chuyển").toUpperCase(Locale.ROOT);
        if (!List.of("GHN", "GHTK", "OTHER").contains(cleanCarrier)) {
            throw new IllegalArgumentException("Chọn GHN, GHTK hoặc đơn vị khác.");
        }
        String tracking;
        do {
            tracking = "ARES-" + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 12).toUpperCase(Locale.ROOT);
        } while (deliveries.existsByTrackingNumber(tracking));

        Delivery delivery = deliveries.save(new Delivery(required(orderCode, "Mã đơn"),
                required(recipientName, "Người nhận"), required(phone, "Số điện thoại"),
                required(address, "Địa chỉ"), required(city, "Tỉnh/thành phố"), cleanCarrier, tracking));
        events.save(new DeliveryEvent(delivery, DeliveryStatus.READY, "Đã tạo vận đơn, chờ bàn giao cho đơn vị vận chuyển."));
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
        delivery.setStatus(next);
        delivery.markUpdated();
        events.save(new DeliveryEvent(delivery, next, description(next, delivery.getCarrier())));
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
}
