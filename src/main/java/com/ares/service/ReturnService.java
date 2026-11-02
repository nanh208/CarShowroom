package com.ares.service;

import com.ares.dto.ReturnDtos.CreateReturnRequest;
import com.ares.dto.ReturnDtos.AdminReturnRecord;
import com.ares.dto.ReturnDtos.ReturnLineRequest;
import com.ares.dto.ReturnDtos.ReturnResponse;
import com.ares.dto.ReturnDtos.ReturnableItem;
import com.ares.dto.ReturnDtos.ReturnableOrder;
import com.ares.dto.ReturnDtos.UpdateReturnStatusRequest;
import com.ares.model.CustomerOrder;
import com.ares.model.OrderLine;
import com.ares.model.ReturnRequest;
import com.ares.model.ReturnRequestItem;
import com.ares.repository.CustomerOrderRepository;
import com.ares.repository.ReturnRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class ReturnService {
    private static final int RETURN_WINDOW_DAYS = 7;
    private static final List<String> OPEN_STATUSES = List.of("PENDING", "APPROVED");

    private final CustomerOrderRepository orders;
    private final ReturnRequestRepository returns;

    public ReturnService(CustomerOrderRepository orders, ReturnRequestRepository returns) {
        this.orders = orders;
        this.returns = returns;
    }

    @Transactional(readOnly = true)
    public ReturnableOrder lookup(String orderCode, String phone) {
        CustomerOrder order = findVerifiedOrder(orderCode, phone);
        validateEligible(order);
        return toReturnableOrder(order);
    }

    @Transactional
    public ReturnResponse create(String orderCode, CreateReturnRequest request) {
        if (request == null) throw new IllegalArgumentException("Thiếu thông tin yêu cầu đổi trả");
        CustomerOrder order = orders.findByCodeForUpdate(orderCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng hoặc thông tin xác thực không khớp"));
        verifyPhone(order, request.phone());
        validateEligible(order);
        if (returns.existsByOrder_IdAndStatusIn(order.getId(), OPEN_STATUSES)) {
            throw new IllegalArgumentException("Đơn hàng đang có yêu cầu đổi trả được xử lý");
        }
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (reason.length() < 10 || reason.length() > 1000) {
            throw new IllegalArgumentException("Vui lòng mô tả lý do từ 10 đến 1000 ký tự");
        }
        String kind = request.kind() == null ? "" : request.kind().trim().toUpperCase(Locale.ROOT);
        if (!List.of("RETURN", "REFUND").contains(kind)) {
            throw new IllegalArgumentException("Vui lòng chọn đổi sản phẩm hoặc trả hàng hoàn tiền");
        }
        Map<String, Integer> requested = aggregate(request.items());
        Map<String, OrderLine> purchased = new HashMap<>();
        for (OrderLine line : order.getItems()) purchased.put(normalizeCode(line.getProductCode()), line);

        Map<String, Integer> previouslyRequested = previouslyRequested(order);
        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setRequestCode("RET-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT));
        returnRequest.setOrder(order);
        returnRequest.setOrderCode(order.getCode());
        returnRequest.setKind(kind);
        returnRequest.setReason(reason);
        returnRequest.setStatus("PENDING");

        long estimate = 0;
        for (Map.Entry<String, Integer> entry : requested.entrySet()) {
            OrderLine line = purchased.get(entry.getKey());
            if (line == null) throw new IllegalArgumentException("Sản phẩm không thuộc đơn hàng này");
            int remaining = line.getQuantity() - previouslyRequested.getOrDefault(entry.getKey(), 0);
            if (entry.getValue() > remaining) throw new IllegalArgumentException("Số lượng đổi trả vượt quá số sản phẩm còn được yêu cầu");
            estimate = Math.addExact(estimate, Math.multiplyExact(line.getUnitPrice(), entry.getValue()));
            returnRequest.addItem(new ReturnRequestItem(line.getProductCode(), line.getCarName(), entry.getValue(), line.getUnitPrice()));
        }
        returnRequest.setEstimatedRefundAmount("REFUND".equals(kind) ? estimate : 0);
        return response(returns.save(returnRequest));
    }

    @Transactional(readOnly = true)
    public ReturnResponse track(String requestCode, String phone) {
        if (requestCode == null || requestCode.isBlank() || phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mã yêu cầu và số điện thoại");
        }
        ReturnRequest request = returns.findByRequestCode(requestCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu hoặc thông tin xác thực không khớp"));
        if (!digits(request.getOrder().getPhone()).equals(digits(phone))) {
            throw new IllegalArgumentException("Không tìm thấy yêu cầu hoặc thông tin xác thực không khớp");
        }
        return response(request);
    }

    @Transactional(readOnly = true)
    public List<AdminReturnRecord> listForAdmin() {
        return returns.findAllByOrderByCreatedAtDesc().stream().map(this::adminRecord).toList();
    }

    @Transactional
    public AdminReturnRecord updateStatus(String requestCode, UpdateReturnStatusRequest update) {
        if (update == null) throw new IllegalArgumentException("Thiếu trạng thái cần cập nhật");
        ReturnRequest request = returns.findByRequestCodeForUpdate(requestCode)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy yêu cầu đổi trả"));
        String next = update.status() == null ? "" : update.status().trim().toUpperCase(Locale.ROOT);
        if (!List.of("APPROVED", "REJECTED", "COMPLETED").contains(next)) {
            throw new IllegalArgumentException("Trạng thái xử lý không hợp lệ");
        }
        String current = request.getStatus();
        boolean validTransition = "PENDING".equals(current) && ("APPROVED".equals(next) || "REJECTED".equals(next))
                || "APPROVED".equals(current) && ("REJECTED".equals(next) || "COMPLETED".equals(next));
        if (!validTransition) throw new IllegalArgumentException("Không thể chuyển yêu cầu từ " + current + " sang " + next);
        String note = update.note() == null ? null : update.note().trim();
        if (note != null && note.length() > 1000) throw new IllegalArgumentException("Ghi chú không được vượt quá 1000 ký tự");
        if ("REJECTED".equals(next) && (note == null || note.isBlank())) {
            throw new IllegalArgumentException("Cần ghi lý do từ chối để khách hàng biết");
        }
        request.setStatus(next);
        request.setAdminNote(note == null || note.isBlank() ? null : note);
        request.setUpdatedAt(LocalDateTime.now());
        return adminRecord(returns.save(request));
    }

    private CustomerOrder findVerifiedOrder(String orderCode, String phone) {
        if (orderCode == null || orderCode.isBlank() || phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mã đơn hàng và số điện thoại đặt hàng");
        }
        CustomerOrder order = orders.findByCode(orderCode.trim())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng hoặc thông tin xác thực không khớp"));
        verifyPhone(order, phone);
        return order;
    }

    private void verifyPhone(CustomerOrder order, String suppliedPhone) {
        if (suppliedPhone == null || !digits(order.getPhone()).equals(digits(suppliedPhone))) {
            throw new IllegalArgumentException("Không tìm thấy đơn hàng hoặc thông tin xác thực không khớp");
        }
    }

    private void validateEligible(CustomerOrder order) {
        if ("CANCELLED".equalsIgnoreCase(order.getStatus())) throw new IllegalArgumentException("Đơn hàng đã hủy không thể đổi trả");
        LocalDateTime deadline = order.getCreatedAt().plusDays(RETURN_WINDOW_DAYS);
        if (LocalDateTime.now().isAfter(deadline)) throw new IllegalArgumentException("Đã quá thời hạn 7 ngày kể từ ngày đặt hàng");
        if (order.getItems().isEmpty()) throw new IllegalArgumentException("Đơn hàng không có sản phẩm để đổi trả");
    }

    private ReturnableOrder toReturnableOrder(CustomerOrder order) {
        Map<String, Integer> previouslyRequested = previouslyRequested(order);
        List<ReturnableItem> items = order.getItems().stream().map(line -> {
            int prior = previouslyRequested.getOrDefault(normalizeCode(line.getProductCode()), 0);
            return new ReturnableItem(line.getProductCode(), line.getCarName(), line.getQuantity(), prior,
                    Math.max(0, line.getQuantity() - prior), line.getUnitPrice());
        }).toList();
        return new ReturnableOrder(order.getCode(), order.getCreatedAt(), order.getCreatedAt().plusDays(RETURN_WINDOW_DAYS),
                items, returns.existsByOrder_IdAndStatusIn(order.getId(), OPEN_STATUSES));
    }

    private Map<String, Integer> previouslyRequested(CustomerOrder order) {
        Map<String, Integer> quantities = new HashMap<>();
        for (ReturnRequest prior : returns.findByOrder_IdOrderByCreatedAtDesc(order.getId())) {
            if ("REJECTED".equals(prior.getStatus())) continue;
            for (ReturnRequestItem item : prior.getItems()) {
                String code = normalizeCode(item.getProductCode());
                quantities.merge(code, item.getQuantity(), Math::addExact);
            }
        }
        return quantities;
    }

    private Map<String, Integer> aggregate(List<ReturnLineRequest> items) {
        if (items == null || items.isEmpty() || items.size() > 30) throw new IllegalArgumentException("Chọn từ 1 đến 30 sản phẩm để gửi yêu cầu");
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (ReturnLineRequest item : items) {
            if (item == null || item.productCode() == null || item.productCode().isBlank()
                    || item.productCode().length() > 80 || item.quantity() == null || item.quantity() < 1) {
                throw new IllegalArgumentException("Sản phẩm hoặc số lượng đổi trả không hợp lệ");
            }
            String code = normalizeCode(item.productCode());
            quantities.merge(code, item.quantity(), Math::addExact);
        }
        return quantities;
    }

    private String digits(String value) { return value == null ? "" : value.replaceAll("\\D", ""); }
    private String normalizeCode(String value) { return value.trim().toUpperCase(Locale.ROOT); }

    private ReturnResponse response(ReturnRequest request) {
        return new ReturnResponse(request.getRequestCode(), request.getOrderCode(), request.getKind(), request.getStatus(),
                request.getEstimatedRefundAmount(), request.getAdminNote(), request.getCreatedAt(), request.getUpdatedAt());
    }

    private AdminReturnRecord adminRecord(ReturnRequest request) {
        List<ReturnableItem> items = request.getItems().stream()
                .map(item -> new ReturnableItem(item.getProductCode(), item.getProductName(), item.getQuantity(), 0,
                        item.getQuantity(), item.getUnitPrice())).toList();
        return new AdminReturnRecord(request.getRequestCode(), request.getOrderCode(), request.getOrder().getCustomerName(),
                request.getOrder().getPhone(), request.getKind(), request.getReason(), request.getStatus(),
                request.getEstimatedRefundAmount(), request.getAdminNote(), request.getCreatedAt(), request.getUpdatedAt(), items);
    }
}
