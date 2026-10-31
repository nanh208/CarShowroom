package com.ares.report.controller;

import com.ares.model.CustomerOrder;
import com.ares.repository.CustomerOrderRepository;
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
@RequestMapping("/reports/orders")
public class OrderReportController {
    private final CustomerOrderRepository orders;

    public OrderReportController(CustomerOrderRepository orders) { this.orders = orders; }

    @GetMapping(value = "/csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> export(HttpSession session) {
        if (session.getAttribute("currentUser") == null) return ResponseEntity.status(401).build();
        DateTimeFormatter timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder csv = new StringBuilder("\uFEFFMã đơn,Ngày tạo,Khách hàng,Số điện thoại,Email,Địa chỉ giao hàng,Trạng thái,Phương thức thanh toán,Trạng thái thanh toán,Thời gian thanh toán,Số lượng,Tổng tiền\r\n");
        for (CustomerOrder order : orders.findAll()) {
            csv.append(cell(order.getCode())).append(',')
                    .append(cell(order.getCreatedAt() == null ? "" : order.getCreatedAt().format(timestamp))).append(',')
                    .append(cell(order.getCustomerName())).append(',').append(cell(order.getPhone())).append(',')
                    .append(cell(order.getEmail())).append(',').append(cell(order.getShippingAddress())).append(',')
                    .append(cell(order.getStatus())).append(',').append(cell(order.getPaymentMethod())).append(',')
                    .append(cell(order.getPaymentStatus())).append(',')
                    .append(cell(order.getPaidAt() == null ? "" : order.getPaidAt().format(timestamp))).append(',')
                    .append(order.getTotalQuantity()).append(',').append(order.getTotal()).append("\r\n");
        }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.csv")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String cell(String value) {
        if (value == null) return "\"\"";
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
