package com.tengyun.product.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalApiGuard {

    private final String expectedToken;

    public InternalApiGuard(@Value("${security.internal-api-token:}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    public void verify(String actualToken) {
        if (expectedToken == null || expectedToken.length() < 32 || actualToken == null
                || !MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                actualToken.getBytes(StandardCharsets.UTF_8))) {
            throw new SecurityException("INTERNAL_ACCESS_DENIED");
        }
    }
}
