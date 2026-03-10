package com.smartauth.security.lockout;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        loginAttemptService = new LoginAttemptService();
    }

    @Test
    void loginSucceeded_ClearsFailedAttempts() {
        // Given
        String email = "test@example.com";
        loginAttemptService.loginFailed(email);
        loginAttemptService.loginFailed(email);

        // When
        loginAttemptService.loginSucceeded(email);

        // Then
        assertThat(loginAttemptService.isBlocked(email)).isFalse();
        assertThat(loginAttemptService.getRemainingAttempts(email)).isEqualTo(5);
    }

    @Test
    void loginFailed_IncrementAttempts() {
        // Given
        String email = "test@example.com";

        // When
        loginAttemptService.loginFailed(email);
        loginAttemptService.loginFailed(email);

        // Then
        assertThat(loginAttemptService.getRemainingAttempts(email)).isEqualTo(3);
        assertThat(loginAttemptService.isBlocked(email)).isFalse();
    }

    @Test
    void loginFailed_ExceedsMaxAttempts_AccountLocked() {
        // Given
        String email = "test@example.com";

        // When - simulate 5 failed attempts
        for (int i = 0; i < 5; i++) {
            loginAttemptService.loginFailed(email);
        }

        // Then
        assertThat(loginAttemptService.isBlocked(email)).isTrue();
        assertThat(loginAttemptService.getRemainingAttempts(email)).isEqualTo(0);
    }

    @Test
    void isBlocked_NoAttempts_ReturnsFalse() {
        // Given
        String email = "newuser@example.com";

        // When / Then
        assertThat(loginAttemptService.isBlocked(email)).isFalse();
    }

    @Test
    void getRemainingAttempts_NoAttempts_ReturnsMax() {
        // Given
        String email = "newuser@example.com";

        // When / Then
        assertThat(loginAttemptService.getRemainingAttempts(email)).isEqualTo(5);
    }
}
