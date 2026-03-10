package com.smartauth.auth.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long accessTokenExpiration = 900000L; // 15 minutes
    private final long refreshTokenExpiration = 604800000L; // 7 days

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", secret);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", accessTokenExpiration);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", refreshTokenExpiration);
    }

    @Test
    void generateAccessToken_Success() {
        // Given
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String role = "ROLE_USER";

        // When
        String token = jwtService.generateAccessToken(userId, email, role);

        // Then
        assertThat(token).isNotNull();
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtService.extractEmail(token)).isEqualTo(email);
        assertThat(jwtService.extractRole(token)).isEqualTo(role);
    }

    @Test
    void generateRefreshToken_Success() {
        // Given
        UUID userId = UUID.randomUUID();

        // When
        String token = jwtService.generateRefreshToken(userId);

        // Then
        assertThat(token).isNotNull();
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void isTokenValid_ExpiredToken_ReturnsFalse() {
        // Given - create an expired token
        UUID userId = UUID.randomUUID();
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        String expiredToken = Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date(System.currentTimeMillis() - 1000000))
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(key)
                .compact();

        // When / Then
        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    void isTokenValid_InvalidToken_ReturnsFalse() {
        // Given
        String invalidToken = "invalid.token.here";

        // When / Then
        assertThat(jwtService.isTokenValid(invalidToken)).isFalse();
    }

    @Test
    void extractClaims_Success() {
        // Given
        UUID userId = UUID.randomUUID();
        String email = "test@example.com";
        String role = "ROLE_ADMIN";
        String token = jwtService.generateAccessToken(userId, email, role);

        // When
        UUID extractedUserId = jwtService.extractUserId(token);
        String extractedEmail = jwtService.extractEmail(token);
        String extractedRole = jwtService.extractRole(token);

        // Then
        assertThat(extractedUserId).isEqualTo(userId);
        assertThat(extractedEmail).isEqualTo(email);
        assertThat(extractedRole).isEqualTo(role);
    }
}
