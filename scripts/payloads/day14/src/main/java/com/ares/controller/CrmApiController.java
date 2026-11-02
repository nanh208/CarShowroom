package com.ares.controller;

import com.ares.dto.ApiResponse;
import com.ares.model.CustomerTicket;
import com.ares.service.CrmService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crm")
@CrossOrigin(originPatterns = "*", allowCredentials = "true")
public class CrmApiController {

    private final CrmService crmService;

    public CrmApiController(CrmService crmService) {
        this.crmService = crmService;
    }

    @PostMapping("/tickets")
    public ResponseEntity<ApiResponse> createInquiry(@RequestBody InquiryRequest req) {
        if (req.customerName() == null || req.phone() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Vui lÃ²ng cung cáº¥p há» tÃªn vÃ  sá»‘ Ä‘iá»‡n thoáº¡i"));
        }
        CustomerTicket ticket = crmService.createInquiry(req.customerName(), req.phone(), req.email(), req.interestCar(), req.inquiryType(), req.notes());
        ApiResponse res = ApiResponse.ok("Gá»­i yÃªu cáº§u tÆ° váº¥n thÃ nh cÃ´ng! Showroom sáº½ liÃªn há»‡ láº¡i ngay.");
        res.setData(ticket);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/admin/tickets")
    public ResponseEntity<ApiResponse> getTickets(@RequestParam(required = false) String status) {
        ApiResponse res = ApiResponse.ok("Danh sÃ¡ch Ticket CRM");
        res.setData(crmService.getAllTickets(status));
        return ResponseEntity.ok(res);
    }

    @PutMapping("/admin/tickets/{id}")
    public ResponseEntity<ApiResponse> updateTicket(@PathVariable Long id, @RequestBody UpdateTicketRequest req) {
        try {
            CustomerTicket t = crmService.updateTicketStatus(id, req.status(), req.assignedStaff(), req.notes());
            ApiResponse res = ApiResponse.ok("Cáº­p nháº­t ticket thÃ nh cÃ´ng");
            res.setData(t);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    public record InquiryRequest(String customerName, String phone, String email, String interestCar, String inquiryType, String notes) {}
    public record UpdateTicketRequest(String status, String assignedStaff, String notes) {}
}
