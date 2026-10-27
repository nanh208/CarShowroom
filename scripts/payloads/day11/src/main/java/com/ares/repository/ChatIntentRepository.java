package com.ares.repository;

import com.ares.model.ChatIntent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatIntentRepository extends JpaRepository<ChatIntent, Long> {
    List<ChatIntent> findByActiveTrue();
}
