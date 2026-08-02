package com.tengyun.agent.controller;

import com.tengyun.agent.dto.ApiResponse;
import com.tengyun.agent.dto.ChatRequest;
import com.tengyun.agent.service.AgentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/agent")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @PostMapping("/chat")
    public ApiResponse<String> chat(
            @RequestHeader("X-User-Id") @Min(value = 1, message = "USER_ID_INVALID") Long userId,
            @Valid @RequestBody ChatRequest request
    ) {
        return new ApiResponse<>(1, "SUCCESS", agentService.chat(userId, request.message()));
    }
}
