package com.tengyun.user.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordServiceTest {

    private final PasswordService passwordService = new PasswordService();

    @Test
    void shouldMatchPlainTextPasswordForLegacyData() {
        assertTrue(passwordService.matches("123456", "123456"));
    }

    @Test
    void shouldMatchBcryptPassword() {
        String encoded = new BCryptPasswordEncoder().encode("123456");
        assertTrue(passwordService.matches("123456", encoded));
    }

    @Test
    void shouldRejectBlankInput() {
        assertFalse(passwordService.matches("", "123456"));
        assertFalse(passwordService.matches("123456", ""));
    }
}
