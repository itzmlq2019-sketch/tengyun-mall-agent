package com.tengyun.gateway.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenProviderTest {

    @Test
    void shouldRejectMissingSecret() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secret", "");

        assertThrows(IllegalStateException.class, provider::init);
    }

    @Test
    void shouldAcceptStrongSecret() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secret", "a-secure-jwt-secret-with-at-least-32-characters");

        assertDoesNotThrow(provider::init);
    }
}
