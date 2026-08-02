package com.tengyun.order.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdminApiGuardTest {

    private static final String TOKEN = "test-admin-token-at-least-32-characters";

    @Test
    void shouldAcceptMatchingToken() {
        AdminApiGuard guard = new AdminApiGuard(TOKEN);
        assertDoesNotThrow(() -> guard.verify(TOKEN));
    }

    @Test
    void shouldFailClosedWhenTokenMissingOrWrong() {
        assertThrows(SecurityException.class, () -> new AdminApiGuard(TOKEN).verify("wrong"));
        assertThrows(SecurityException.class, () -> new AdminApiGuard("").verify(TOKEN));
    }
}
