package com.tengyun.product.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InternalApiGuardTest {

    private static final String TOKEN = "test-internal-token-at-least-32-characters";

    @Test
    void shouldAcceptMatchingToken() {
        InternalApiGuard guard = new InternalApiGuard(TOKEN);
        assertDoesNotThrow(() -> guard.verify(TOKEN));
    }

    @Test
    void shouldFailClosedWhenTokenMissingOrWrong() {
        assertThrows(SecurityException.class, () -> new InternalApiGuard(TOKEN).verify("wrong"));
        assertThrows(SecurityException.class, () -> new InternalApiGuard("").verify(TOKEN));
    }
}
