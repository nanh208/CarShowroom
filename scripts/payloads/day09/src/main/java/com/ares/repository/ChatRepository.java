package com.ares.repository;

import com.ares.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySessionIdOrderByIdAsc(String sessionId);
    List<ChatMessage> findBySessionIdAndIdGreaterThanOrderByIdAsc(String sessionId, Long afterId);
    ChatMessage findFirstBySessionIdOrderByIdDesc(String sessionId);

    @Query("SELECT DISTINCT c.sessionId FROM ChatMessage c ORDER BY c.sessionId")
    List<String> findDistinctSessions();
}
