package com.smartauth.security.lockout;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 15;

    private final Map<String, LoginAttempt> attemptsCache = new ConcurrentHashMap<>();

    public void loginSucceeded(String email) {
        attemptsCache.remove(email);
        log.debug("Login attempt cache cleared for: {}", email);
    }

    public void loginFailed(String email) {
        LoginAttempt attempt = attemptsCache.computeIfAbsent(email, k -> new LoginAttempt());
        attempt.incrementFailedAttempts();

        if (attempt.getFailedAttempts() >= MAX_ATTEMPTS) {
            attempt.setLockedUntil(OffsetDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES));
            log.warn("Account locked due to excessive failed login attempts: {}", email);
        } else {
            log.debug("Failed login attempt {} for: {}", attempt.getFailedAttempts(), email);
        }
    }

    public boolean isBlocked(String email) {
        LoginAttempt attempt = attemptsCache.get(email);
        
        if (attempt == null) {
            return false;
        }

        if (attempt.isLocked()) {
            log.debug("Login blocked for locked account: {}", email);
            return true;
        }

        // If lock expired, clear the attempt
        if (attempt.getLockedUntil() != null && !attempt.isLocked()) {
            attemptsCache.remove(email);
            log.debug("Lock expired and cleared for: {}", email);
        }

        return false;
    }

    public int getRemainingAttempts(String email) {
        LoginAttempt attempt = attemptsCache.get(email);
        if (attempt == null) {
            return MAX_ATTEMPTS;
        }
        return Math.max(0, MAX_ATTEMPTS - attempt.getFailedAttempts());
    }
}
