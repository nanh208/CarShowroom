package com.ares.service;

import com.ares.model.User;
import com.ares.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TotpAttemptService {
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;
    private final UserRepository users;

    public TotpAttemptService(UserRepository users) {
        this.users = users;
    }

    @Transactional
    public synchronized boolean verify(User user, String code) {
        LocalDateTime now = LocalDateTime.now();
        if (user.getTotpLockedUntil() != null && user.getTotpLockedUntil().isAfter(now)) return false;
        if (user.getTotpLockedUntil() != null) {
            user.setTotpLockedUntil(null);
            user.setTotpFailedAttempts(0);
        }
        if (TotpService.verify(user.getTotpSecret(), code)) {
            user.setTotpFailedAttempts(0);
            user.setTotpLockedUntil(null);
            users.save(user);
            return true;
        }
        int attempts = user.getTotpFailedAttempts() + 1;
        user.setTotpFailedAttempts(attempts);
        if (attempts >= MAX_ATTEMPTS) {
            user.setTotpLockedUntil(now.plusMinutes(LOCK_MINUTES));
        }
        users.save(user);
        return false;
    }

    public void reset(User user) {
        user.setTotpFailedAttempts(0);
        user.setTotpLockedUntil(null);
    }
}
