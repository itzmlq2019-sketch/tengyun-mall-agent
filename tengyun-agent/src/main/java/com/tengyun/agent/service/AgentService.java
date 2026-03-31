package com.tengyun.agent.service;

import reactor.core.publisher.Flux;

public interface AgentService {
    /**
     * AI 导购流式对话核心逻辑
     * @param userId 用户ID，用于处理上下文记忆
     * @param message 用户输入的原始消息
     * @return 返回 SSE 字符串流
     */
    Flux<String> chatStream(Long userId, String message);
}