package com.ares.service;

import com.ares.model.CustomerTicket;
import com.ares.repository.CustomerTicketRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class CrmService {

    private final CustomerTicketRepository ticketRepository;

    public CrmService(CustomerTicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public CustomerTicket createInquiry(String name, String phone, String email, String car, String type, String notes) {
        CustomerTicket t = new CustomerTicket();
        String code = "TKT-" + DateTimeFormatter.ofPattern("yyMMdd").format(LocalDateTime.now()) + "-" + (System.currentTimeMillis() % 10000);
        t.setTicketCode(code);
        t.setCustomerName(name);
        t.setPhone(phone);
        t.setEmail(email);
        t.setInterestCar(car);
        t.setInquiryType(type != null ? type : "CONSULTATION");
        t.setNotes(notes);
        t.setStatus("NEW");
        return ticketRepository.save(t);
    }

    public List<CustomerTicket> getAllTickets(String status) {
        if (status != null && !status.isBlank()) {
            return ticketRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return ticketRepository.findAll();
    }

    public CustomerTicket updateTicketStatus(Long id, String status, String staff, String notes) {
        CustomerTicket t = ticketRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("KhÃ´ng tÃ¬m tháº¥y ticket #" + id));
        if (status != null) t.setStatus(status);
        if (staff != null) t.setAssignedStaff(staff);
        if (notes != null) t.setNotes(notes);
        return ticketRepository.save(t);
    }
}
