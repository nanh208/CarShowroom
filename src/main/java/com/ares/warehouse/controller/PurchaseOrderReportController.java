package com.ares.warehouse.controller;

import com.ares.warehouse.model.PurchaseOrder;
import com.ares.warehouse.service.PurchaseOrderService;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/inventory/purchase-orders")
public class PurchaseOrderReportController {
    private final PurchaseOrderService service;

    public PurchaseOrderReportController(PurchaseOrderService service) { this.service = service; }

    @GetMapping(value = "/export.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> export(HttpSession session) {
        if (session.getAttribute("currentUser") == null) return ResponseEntity.status(401).build();
        DateTimeFormatter timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder csv = new StringBuilder("\uFEFFMã đơn nhập,Nhà cung cấp,Mã kho,Trạng thái,Ngày tạo,Ngày đặt,Ngày nhận,SKU,Mặt hàng,Số lượng,Đơn giá,Thành tiền,Tổng đơn,Ghi chú\r\n");
        for (PurchaseOrder order : service.getOrders()) {
            for (var line : order.getLines()) {
                csv.append(cell(order.getCode())).append(',').append(cell(order.getSupplier())).append(',')
                        .append(cell(order.getWarehouse().getCode())).append(',').append(cell(order.getStatus().name())).append(',')
                        .append(cell(format(order.getCreatedAt(), timestamp))).append(',')
                        .append(cell(format(order.getOrderedAt(), timestamp))).append(',')
                        .append(cell(format(order.getReceivedAt(), timestamp))).append(',')
                        .append(cell(line.getItem().getSku())).append(',').append(cell(line.getItem().getName())).append(',')
                        .append(line.getQuantity()).append(',').append(line.getUnitCost()).append(',')
                        .append(line.getLineTotal()).append(',').append(order.getTotalCost()).append(',')
                        .append(cell(order.getNote())).append("\r\n");
            }
        }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=purchase-orders.csv")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String format(java.time.LocalDateTime value, DateTimeFormatter formatter) {
        return value == null ? "" : value.format(formatter);
    }

    private static String cell(String value) {
        if (value == null) return "\"\"";
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
