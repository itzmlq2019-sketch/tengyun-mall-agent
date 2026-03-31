package com.tengyun.agent.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAuditLog {
    private Long id;
    private Long userId;
    private String userMessage;   // 用户问了什么
    private String aiResponse;    // AI 最终答了什么
    private String model;         // 调用的模型（如 deepseek-chat）
    private Integer promptTokens; // 输入消耗
    private Integer completionTokens; // 输出消耗
    private Integer totalTokens;  // 总消耗
    private Double cost;          // 预估人民币成本
    private Long latency;         // 响应耗时(ms)
    private LocalDateTime createTime;
}
