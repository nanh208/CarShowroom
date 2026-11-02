package com.ares.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String sessionId;

    private String customerName;

    private Boolean fromAdmin = false;

    @Column(nullable = false, length = 2000)
    private String content;

    private Boolean readByStaff = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    public ChatMessage() {}

    public ChatMessage(String sessionId, String customerName, Boolean fromAdmin, String content) {
        this.sessionId = sessionId;
        this.customerName = customerName;
        this.fromAdmin = fromAdmin;
        this.content = content;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public Boolean getFromAdmin() { return fromAdmin; }
    public void setFromAdmin(Boolean fromAdmin) { this.fromAdmin = fromAdmin; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Boolean getReadByStaff() { return readByStaff; }
    public void setReadByStaff(Boolean readByStaff) { this.readByStaff = readByStaff; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
