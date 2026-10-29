package com.ares.repository;

import com.ares.model.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    List<UserPreference> findByUser_IdOrderByPreferenceKeyAsc(Long userId);
    Optional<UserPreference> findByUser_IdAndPreferenceKey(Long userId, String preferenceKey);
}
