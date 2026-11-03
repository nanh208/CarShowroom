package com.ares.transport.controller;

import com.ares.model.User;
import com.ares.transport.model.Delivery;
import com.ares.transport.model.DeliveryStatus;
import com.ares.transport.service.DeliveryLabelService;
import com.ares.transport.service.CarrierQuoteService;
import com.ares.transport.service.DeliveryService;
import com.ares.service.FraudReviewService;
import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/transport/deliveries")
public class DeliveryController {
    private final DeliveryService service;
    private final DeliveryLabelService labels;
    private final CarrierQuoteService quotes;
    private final FraudReviewService fraudReview;

    public DeliveryController(DeliveryService service, DeliveryLabelService labels, CarrierQuoteService quotes,
                              FraudReviewService fraudReview) {
        this.service = service;
        this.labels = labels;
        this.quotes = quotes;
        this.fraudReview = fraudReview;
    }

    @GetMapping
    public String page(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        model.addAttribute("deliveries", service.getDeliveries());
        model.addAttribute("pendingOrders", service.getPendingOrders());
        model.addAttribute("fraudQueue", fraudReview.getQueue());
        return "transport/deliveries";
    }


    @GetMapping(value = "/export.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> export(HttpSession session) {
        if (session.getAttribute("currentUser") == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        DateTimeFormatter timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder csv = new StringBuilder("\uFEFFMã đơn,Mã vận đơn,Người nhận,Số điện thoại,Địa chỉ,Tỉnh/thành phố,Đơn vị vận chuyển,Trạng thái,Tạo lúc,Cập nhật lúc\r\n");
        for (Delivery delivery : service.getDeliveries()) {
            csv.append(cell(delivery.getOrderCode())).append(',').append(cell(delivery.getTrackingNumber()))
                    .append(',').append(cell(delivery.getRecipientName())).append(',').append(cell(delivery.getPhone()))
                    .append(',').append(cell(delivery.getAddress())).append(',').append(cell(delivery.getCity()))
                    .append(',').append(cell(delivery.getCarrier())).append(',').append(cell(delivery.getStatus().getLabel()))
                    .append(',').append(cell(delivery.getCreatedAt() == null ? "" : delivery.getCreatedAt().format(timestamp)))
                    .append(',').append(cell(delivery.getUpdatedAt() == null ? "" : delivery.getUpdatedAt().format(timestamp)))
                    .append("\r\n");
        }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=deliveries.csv")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String cell(String value) {
        if (value == null) return "\"\"";
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) value = "'" + value;
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    @PostMapping
    public String create(HttpSession session, @RequestParam String orderCode,
                         @RequestParam String recipientName, @RequestParam String phone,
                         @RequestParam String address, @RequestParam String city,
                         @RequestParam String carrier, RedirectAttributes flash) {
        if (session.getAttribute("currentUser") == null) return "redirect:/login";
        return perform(() -> service.create(orderCode, recipientName, phone, address, city, carrier),
                "Đã tạo vận đơn.", flash);
    }

    @PostMapping("/from-order")
    public String createFromOrder(HttpSession session, @RequestParam Long orderId,
                                  @RequestParam String city, @RequestParam String carrier,
                                  RedirectAttributes flash) {
        if (session.getAttribute("currentUser") == null) return "redirect:/login";
        return perform(() -> service.createFromOrder(orderId, city, carrier),
                "Đã tạo vận đơn từ đơn hàng.", flash);
    }

    @GetMapping("/quotes")
    @ResponseBody
    public ResponseEntity<?> quote(HttpSession session, @RequestParam String city,
                                   @RequestParam int weightGrams, @RequestParam long codAmount) {
        if (session.getAttribute("currentUser") == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(quotes.quoteAll(city, weightGrams, codAmount));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/status")
    public String updateStatus(HttpSession session, @RequestParam Long id,
                               @RequestParam DeliveryStatus status, RedirectAttributes flash) {
        if (session.getAttribute("currentUser") == null) return "redirect:/login";
        return perform(() -> service.updateStatus(id, status), "Đã cập nhật trạng thái vận đơn.", flash);
    }

    @PostMapping("/fraud/{orderId}/approve")
    public String approveRisk(HttpSession session, @PathVariable Long orderId, RedirectAttributes flash) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) return "redirect:/login";
        return perform(() -> fraudReview.approve(orderId, user.getUsername()),
                "Đã duyệt đơn sau khi rà soát rủi ro.", flash);
    }

    @PostMapping("/fraud/{orderId}/reject")
    public String rejectRisk(HttpSession session, @PathVariable Long orderId,
                             @RequestParam(required = false) String note, RedirectAttributes flash) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) return "redirect:/login";
        return perform(() -> fraudReview.reject(orderId, user.getUsername(), note),
                "Đã từ chối đơn và hủy đơn chưa giao.", flash);
    }

    @GetMapping("/{id}/label")
    public ResponseEntity<byte[]> label(HttpSession session, @PathVariable Long id) throws IOException, DocumentException {
        if (session.getAttribute("currentUser") == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Delivery delivery = service.findDelivery(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy vận đơn."));
        byte[] pdf = labels.render(delivery);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=delivery-" + delivery.getTrackingNumber() + ".pdf")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(pdf);
    }

    @GetMapping("/labels")
    public ResponseEntity<byte[]> labels(HttpSession session, @RequestParam List<Long> ids)
            throws IOException, DocumentException {
        if (session.getAttribute("currentUser") == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        List<Long> selectedIds = new ArrayList<>(new LinkedHashSet<>(ids));
        if (selectedIds.isEmpty() || selectedIds.size() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chọn từ 1 đến 100 vận đơn để in nhãn.");
        }
        List<Delivery> selected = selectedIds.stream()
                .map(id -> service.findDelivery(id).orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy vận đơn " + id + ".")))
                .toList();
        byte[] pdf = labels.render(selected);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=delivery-labels.pdf")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(pdf);
    }

    private String perform(Runnable action, String success, RedirectAttributes flash) {
        try {
            action.run();
            flash.addFlashAttribute("successMessage", success);
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/transport/deliveries";
    }
}
