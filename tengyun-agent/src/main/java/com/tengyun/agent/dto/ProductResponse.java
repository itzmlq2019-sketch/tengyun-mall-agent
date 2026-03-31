package com.tengyun.agent.dto;

import java.math.BigDecimal;

/**
 * 你从数据库/远程接口查到的结果，AI 会阅读这些信息并组织语言回复用户
 */
public record ProductResponse(
        String name,
        BigDecimal price,
        Integer stock,
        String message // 用于存放“商品不存在”等提示信息
) {}