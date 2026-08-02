package com.tengyun.user.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final String DEFAULT_SECRET = "TengYunMallSecretKeyMustBeAtLeast256BitsLong==";

    @Value("${security.jwt.secret:" + DEFAULT_SECRET + "}")
    private String secret;

    @Value("${security.jwt.expire-hours:2}")
    private long expireHours;

    private Key key;

    @PostConstruct
    public void init() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("security.jwt.secret must be at least 32 characters");
        }
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String username) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + getExpireMillis()))
                .signWith(key)
                .compact();
    }

    public long getExpireSeconds() {
        return getExpireMillis() / 1000;
    }

    private long getExpireMillis() {
        return expireHours * 60 * 60 * 1000;
    }
}
