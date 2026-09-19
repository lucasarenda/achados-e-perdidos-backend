package com.lostandfound.backend.service;

import com.lostandfound.backend.dto.request.LoginRequest;
import com.lostandfound.backend.dto.request.RegisterRequest;
import com.lostandfound.backend.dto.response.AuthResponse;
import com.lostandfound.backend.exception.BadRequestException;
import com.lostandfound.backend.model.User;
import com.lostandfound.backend.repository.UserRepository;
import com.lostandfound.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("Lucas Silveira", "lucas@example.com", "secret123");
    }

    @Test
    @DisplayName("registers a new user and returns a JWT")
    void registersNewUser() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            user.setCreatedAt(LocalDateTime.now());
            return user;
        });
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        AuthResponse response = authService.register(registerRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("lucas@example.com");
        assertThat(response.getUserId()).isEqualTo(1L);
        verify(passwordEncoder).encode("secret123");
    }

    @Test
    @DisplayName("throws BadRequestException when the email is already registered")
    void rejectsDuplicateEmail() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("logs in a user with valid credentials and returns a JWT")
    void logsInValidUser() {
        LoginRequest loginRequest = new LoginRequest("lucas@example.com", "secret123");

        User user = User.builder()
                .id(1L)
                .name("Lucas Silveira")
                .email("lucas@example.com")
                .password("hashed-password")
                .build();

        when(userRepository.findByEmail("lucas@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token");
        assertThat(response.getUserId()).isEqualTo(1L);
        verify(authenticationManager).authenticate(any());
    }
}
