package com.smartauth.auth.service;

import com.smartauth.auth.domain.RefreshToken;
import com.smartauth.auth.repository.RefreshTokenRepository;
import com.smartauth.common.exception.UnauthorizedException;
import com.smartauth.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Transactional
    public RefreshToken createRefreshToken(User user, String rawToken, String deviceInfo) {
        String tokenHash = hashToken(rawToken);

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(OffsetDateTime.now().plusDays(7))
                .deviceInfo(deviceInfo)
                .build();

        return refreshTokenRepository.save(token);
    }

    @Transactional
    public RefreshToken validateAndGetRefreshToken(String rawToken) {
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found in database");
                    return new UnauthorizedException("Invalid refresh token");
                });

        if (refreshToken.isRevoked()) {
            log.warn("Attempted to use revoked refresh token for user: {}", refreshToken.getUser().getEmail());
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (refreshToken.isExpired()) {
            log.warn("Expired refresh token for user: {}", refreshToken.getUser().getEmail());
            throw new UnauthorizedException("Refresh token has expired");
        }

        // Validate the JWT structure
        if (!jwtService.isTokenValid(rawToken)) {
            log.warn("Invalid JWT structure for refresh token");
            throw new UnauthorizedException("Invalid refresh token format");
        }

        UUID userIdFromToken = jwtService.extractUserId(rawToken);
        if (!refreshToken.getUser().getId().equals(userIdFromToken)) {
            log.error("User ID mismatch in refresh token");
            throw new UnauthorizedException("Invalid refresh token");
        }

        return refreshToken;
    }

    @Transactional
    public void revokeRefreshToken(String rawToken) {
        String tokenHash = hashToken(rawToken);
        
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevokedAt(OffsetDateTime.now());
            refreshTokenRepository.save(token);
            log.info("Refresh token revoked for user: {}", token.getUser().getEmail());
        });
    }

    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        List<RefreshToken> tokens = refreshTokenRepository.findAllByUser_Id(userId);
        OffsetDateTime now = OffsetDateTime.now();
        
        tokens.forEach(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(now);
            }
        });
        
        refreshTokenRepository.saveAll(tokens);
        log.info("All refresh tokens revoked for user ID: {}", userId);
    }

    @Transactional
    public void rotateRefreshToken(RefreshToken oldToken) {
        oldToken.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(oldToken);
        log.debug("Refresh token rotated for user: {}", oldToken.getUser().getEmail());
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }
}
