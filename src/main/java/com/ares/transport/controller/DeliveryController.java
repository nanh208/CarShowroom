package com.ares.transport.controller;

import com.ares.model.User;
import com.ares.transport.model.Delivery;
import com.ares.transport.model.DeliveryStatus;
import com.ares.transport.service.DeliveryLabelService;
import com.ares.transport.service.DeliveryService;
import com.lowagie.text.DocumentException;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/transport/deliveries")
public class DeliveryController {
    private final DeliveryService service;
    private final DeliveryLabelService labels;

    public DeliveryController(DeliveryService service, DeliveryLabelService labels) {
        this.service = service;
        this.labels = labels;
    }

    @GetMapping
    public String page(HttpSession session, Model model) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        model.addAttribute("deliveries", service.getDeliveries());
        return "transport/deliveries";
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

    @PostMapping("/status")
    public String updateStatus(HttpSession session, @RequestParam Long id,
                               @RequestParam DeliveryStatus status, RedirectAttributes flash) {
        if (session.getAttribute("currentUser") == null) return "redirect:/login";
        return perform(() -> service.updateStatus(id, status), "Đã cập nhật trạng thái vận đơn.", flash);
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
