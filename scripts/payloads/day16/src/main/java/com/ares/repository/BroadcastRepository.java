package com.ares.repository;

import com.ares.model.BroadcastMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BroadcastRepository extends JpaRepository<BroadcastMessage, Long> {
    List<BroadcastMessage> findAllByOrderByCreatedAtDesc();
}
