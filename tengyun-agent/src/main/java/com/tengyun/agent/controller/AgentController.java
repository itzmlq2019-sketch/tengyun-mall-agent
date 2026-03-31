package com.tengyun.agent.controller;

import com.tengyun.agent.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/agent")
@CrossOrigin
public class AgentController {

    @Autowired
    private AgentService agentService; // 只注入大脑 Service

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestHeader("X-User-Id") Long userId,
                                   @RequestParam("message") String message,
                                   HttpServletResponse response) {

        // 1. 设置基础响应编码
        response.setCharacterEncoding("UTF-8");

        // 2. 核心逻辑直接甩给 Service，Controller 层只需返回 Flux 流即可
        return agentService.chatStream(userId, message);
    }
}