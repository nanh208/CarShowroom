package com.ares.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences", uniqueConstraints =
        @UniqueConstraint(name = "uk_user_preference_key", columnNames = {"user_id", "preference_key"}))
public class UserPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "preference_key", nullable = false, length = 32)
    private String preferenceKey;

    @Column(name = "preference_value", nullable = false, length = 16)
    private String value;

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getPreferenceKey() { return preferenceKey; }
    public void setPreferenceKey(String preferenceKey) { this.preferenceKey = preferenceKey; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
