package com.ares.transport.controller;

import com.ares.transport.service.DeliveryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/transport/track")
public class PublicTrackingController {
    private final DeliveryService service;

    public PublicTrackingController(DeliveryService service) { this.service = service; }

    @GetMapping
    public String track(@RequestParam(required = false) String trackingNumber, Model model) {
        model.addAttribute("trackingNumber", trackingNumber == null ? "" : trackingNumber.trim());
        if (trackingNumber != null && !trackingNumber.isBlank()) {
            model.addAttribute("tracking", service.findTracking(trackingNumber).orElse(null));
            model.addAttribute("searched", true);
        }
        return "transport/track";
    }
}
