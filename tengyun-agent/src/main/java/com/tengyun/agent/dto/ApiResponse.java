package com.tengyun.agent.dto;

public record ApiResponse<T>(int code, String message, T data) {
}
