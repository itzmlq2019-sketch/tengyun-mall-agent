package com.tengyun.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "MESSAGE_REQUIRED")
        @Size(max = 2000, message = "MESSAGE_TOO_LONG")
        String message
) {
}
