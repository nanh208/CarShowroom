package com.ares.repository;

import com.ares.model.CustomerTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerTicketRepository extends JpaRepository<CustomerTicket, Long> {
    List<CustomerTicket> findByStatusOrderByCreatedAtDesc(String status);
    List<CustomerTicket> findByAssignedStaffOrderByCreatedAtDesc(String staff);
}
