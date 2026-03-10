package com.smartauth.auth.service;

import com.smartauth.auth.domain.RefreshToken;
import com.smartauth.auth.dto.AuthResponse;
import com.smartauth.auth.dto.LoginRequest;
import com.smartauth.auth.dto.RegisterRequest;
import com.smartauth.common.exception.BadRequestException;
import com.smartauth.common.exception.UnauthorizedException;
import com.smartauth.security.lockout.LoginAttemptService;
import com.smartauth.user.domain.Role;
import com.smartauth.user.domain.User;
import com.smartauth.user.repository.RoleRepository;
import com.smartauth.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: email already exists: {}", request.getEmail());
            throw new BadRequestException("Email already registered");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("Default role ROLE_USER not found"));

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isEnabled(true)
                .roles(Set.of(userRole))
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // Check if account is locked
        if (loginAttemptService.isBlocked(request.getEmail())) {
            log.warn("Login blocked: account is locked: {}", request.getEmail());
            throw new UnauthorizedException("Account is temporarily locked due to multiple failed login attempts. Please try again later.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found: {}", request.getEmail());
                    loginAttemptService.loginFailed(request.getEmail());
                    return new UnauthorizedException("Invalid credentials");
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("Login failed: invalid password for user: {}", request.getEmail());
            loginAttemptService.loginFailed(request.getEmail());
            throw new UnauthorizedException("Invalid credentials");
        }

        if (!user.isEnabled()) {
            log.warn("Login failed: account disabled: {}", request.getEmail());
            throw new UnauthorizedException("Account is disabled");
        }

        loginAttemptService.loginSucceeded(request.getEmail());
        log.info("User logged in successfully: {}", user.getEmail());
        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshAccessToken(String refreshTokenValue) {
        log.info("Refresh token attempt");

        RefreshToken refreshToken = refreshTokenService.validateAndGetRefreshToken(refreshTokenValue);
        User user = refreshToken.getUser();

        // Rotate the refresh token (revoke old one)
        refreshTokenService.rotateRefreshToken(refreshToken);

        // Generate new tokens
        String primaryRole = user.getRoles().stream()
                .findFirst()
                .map(Role::getName)
                .orElse("ROLE_USER");

        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), primaryRole);
        String newRefreshToken = jwtService.generateRefreshToken(user.getId());

        refreshTokenService.createRefreshToken(user, newRefreshToken, null);

        log.info("Tokens refreshed successfully for user: {}", user.getEmail());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration() / 1000)
                .build();
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        log.info("Logout attempt");
        refreshTokenService.revokeRefreshToken(refreshTokenValue);
        log.info("Logout successful");
    }

    private AuthResponse createAuthResponse(User user) {
        String primaryRole = user.getRoles().stream()
                .findFirst()
                .map(Role::getName)
                .orElse("ROLE_USER");

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), primaryRole);
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        refreshTokenService.createRefreshToken(user, refreshToken, null);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration() / 1000)
                .build();
    }
}
