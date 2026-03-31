package com.tengyun.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * AI 调用工具时，会根据这个类的字段描述来提取用户话语中的参数
 */
public record CheckoutRequest(
        @JsonPropertyDescription("用户ID") Long userId,
        @JsonPropertyDescription("要结账的商品ID") Long productId,
        @JsonPropertyDescription("购买数量") Integer quantity
) {}