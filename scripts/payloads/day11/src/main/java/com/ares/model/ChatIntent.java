package com.ares.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_intents")
public class ChatIntent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String intentName;

    @Column(length = 1000)
    private String keywords;

    @Column(length = 3000, nullable = false)
    private String responseContent;

    @Column(length = 500)
    private String quickReplies;

    private Boolean active = true;

    private Integer hitCount = 0;

    private LocalDateTime updatedAt = LocalDateTime.now();

    public ChatIntent() {}

    public ChatIntent(String intentName, String keywords, String responseContent, String quickReplies) {
        this.intentName = intentName;
        this.keywords = keywords;
        this.responseContent = responseContent;
        this.quickReplies = quickReplies;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getIntentName() { return intentName; }
    public void setIntentName(String intentName) { this.intentName = intentName; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getResponseContent() { return responseContent; }
    public void setResponseContent(String responseContent) { this.responseContent = responseContent; }
    public String getQuickReplies() { return quickReplies; }
    public void setQuickReplies(String quickReplies) { this.quickReplies = quickReplies; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Integer getHitCount() { return hitCount; }
    public void setHitCount(Integer hitCount) { this.hitCount = hitCount; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
