package com.lostandfound.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtService")
class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret",
                "test-secret-key-for-unit-tests-must-be-long-enough-0123456789");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3600000L); // 1 hour
    }

    private UserDetails userDetails(String email) {
        return User.withUsername(email).password("irrelevant").authorities("ROLE_USER").build();
    }

    @Test
    @DisplayName("generates a token that contains the user's email as subject")
    void generatesTokenWithCorrectSubject() {
        UserDetails user = userDetails("lucas@example.com");

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("lucas@example.com");
    }

    @Test
    @DisplayName("validates a token generated for the same user")
    void validatesTokenForSameUser() {
        UserDetails user = userDetails("lucas@example.com");
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    @DisplayName("rejects a token when it was issued for a different user")
    void rejectsTokenForDifferentUser() {
        UserDetails original = userDetails("lucas@example.com");
        UserDetails another = userDetails("other@example.com");
        String token = jwtService.generateToken(original);

        assertThat(jwtService.isTokenValid(token, another)).isFalse();
    }

    @Test
    @DisplayName("rejects an expired token")
    void rejectsExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L); // already expired
        UserDetails user = userDetails("lucas@example.com");
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isFalse();
    }
}
