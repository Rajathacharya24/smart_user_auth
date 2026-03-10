package com.smartauth.auth.service;

import com.smartauth.auth.dto.LoginRequest;
import com.smartauth.auth.dto.RegisterRequest;
import com.smartauth.auth.dto.AuthResponse;
import com.smartauth.common.exception.BadRequestException;
import com.smartauth.common.exception.UnauthorizedException;
import com.smartauth.security.lockout.LoginAttemptService;
import com.smartauth.user.domain.Role;
import com.smartauth.user.domain.User;
import com.smartauth.user.repository.RoleRepository;
import com.smartauth.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthService authService;

    private Role userRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        userRole = Role.builder()
                .id(1L)
                .name("ROLE_USER")
                .build();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .passwordHash("$2a$12$hashedPassword")
                .isEnabled(true)
                .roles(Set.of(userRole))
                .build();
    }

    @Test
    void register_Success() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .name("John Doe")
                .email("john@example.com")
                .password("SecurePass123!")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateAccessToken(any(), anyString(), anyString())).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(any())).thenReturn("refreshToken");
        when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);

        // When
        AuthResponse response = authService.register(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("accessToken");
        assertThat(response.getRefreshToken()).isEqualTo("refreshToken");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        verify(userRepository).save(any(User.class));
        verify(refreshTokenService).createRefreshToken(any(), anyString(), any());
    }

    @Test
    void register_EmailAlreadyExists_ThrowsBadRequestException() {
        // Given
        RegisterRequest request = RegisterRequest.builder()
                .name("John Doe")
                .email("existing@example.com")
                .password("SecurePass123!")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_Success() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("correctPassword")
                .build();

        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getPassword(), testUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(any(), anyString(), anyString())).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(any())).thenReturn("refreshToken");
        when(jwtService.getAccessTokenExpiration()).thenReturn(900000L);

        // When
        AuthResponse response = authService.login(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isNotNull();
        verify(loginAttemptService).loginSucceeded(request.getEmail());
    }

    @Test
    void login_InvalidPassword_ThrowsUnauthorizedException() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("wrongPassword")
                .build();

        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(request.getPassword(), testUser.getPasswordHash())).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid credentials");

        verify(loginAttemptService).loginFailed(request.getEmail());
        verify(loginAttemptService, never()).loginSucceeded(anyString());
    }

    @Test
    void login_UserNotFound_ThrowsUnauthorizedException() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("password")
                .build();

        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid credentials");

        verify(loginAttemptService).loginFailed(request.getEmail());
    }

    @Test
    void login_AccountLocked_ThrowsUnauthorizedException() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("password")
                .build();

        when(loginAttemptService.isBlocked(request.getEmail())).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("temporarily locked");

        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void login_AccountDisabled_ThrowsUnauthorizedException() {
        // Given
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("correctPassword")
                .build();

        User disabledUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .passwordHash("$2a$12$hashedPassword")
                .isEnabled(false)
                .roles(Set.of(userRole))
                .build();

        when(loginAttemptService.isBlocked(anyString())).thenReturn(false);
        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(disabledUser));
        when(passwordEncoder.matches(request.getPassword(), disabledUser.getPasswordHash())).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Account is disabled");
    }
}
