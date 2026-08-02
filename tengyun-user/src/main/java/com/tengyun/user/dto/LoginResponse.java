package com.tengyun.user.dto;

public record LoginResponse(String token, String tokenType, long expiresInSeconds) {
}
