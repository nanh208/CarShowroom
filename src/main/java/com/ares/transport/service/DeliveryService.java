package com.ares.transport.service;

import com.ares.transport.model.Delivery;
import com.ares.transport.model.DeliveryStatus;
import com.ares.transport.repository.DeliveryRepository;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryService {
    private final DeliveryRepository deliveries;

    public DeliveryService(DeliveryRepository deliveries) { this.deliveries = deliveries; }

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

        deliveries.save(new Delivery(required(orderCode, "Mã đơn"),
                required(recipientName, "Người nhận"), required(phone, "Số điện thoại"),
                required(address, "Địa chỉ"), required(city, "Tỉnh/thành phố"), cleanCarrier, tracking));
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
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " không được để trống.");
        return value.trim();
    }

    private static String label(DeliveryStatus status) {
        return switch (status) {
            case READY -> "Chờ bàn giao";
            case IN_TRANSIT -> "Đang vận chuyển";
            case OUT_FOR_DELIVERY -> "Đang giao";
            case DELIVERED -> "Đã giao";
            case RETURNED -> "Hoàn hàng";
        };
    }
}
