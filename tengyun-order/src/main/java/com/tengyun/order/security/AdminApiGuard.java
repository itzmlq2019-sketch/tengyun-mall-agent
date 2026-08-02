package com.tengyun.order.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class AdminApiGuard {

    private final String expectedToken;

    public AdminApiGuard(@Value("${security.admin-api-token:}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    public void verify(String actualToken) {
        if (expectedToken == null || expectedToken.length() < 32 || actualToken == null
                || !MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                actualToken.getBytes(StandardCharsets.UTF_8))) {
            throw new SecurityException("ADMIN_ACCESS_DENIED");
        }
    }
}
