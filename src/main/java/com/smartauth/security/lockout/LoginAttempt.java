package com.smartauth.security.lockout;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttempt {
    private int failedAttempts;
    private OffsetDateTime lastAttemptTime;
    private OffsetDateTime lockedUntil;

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(OffsetDateTime.now());
    }

    public void incrementFailedAttempts() {
        this.failedAttempts++;
        this.lastAttemptTime = OffsetDateTime.now();
    }

    public void reset() {
        this.failedAttempts = 0;
        this.lastAttemptTime = null;
        this.lockedUntil = null;
    }
}
