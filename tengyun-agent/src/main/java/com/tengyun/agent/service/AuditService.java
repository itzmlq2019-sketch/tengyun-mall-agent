package com.tengyun.agent.service;

import com.tengyun.agent.entity.AiAuditLog;
import com.tengyun.agent.mapper.AiAuditMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    @Autowired
    private AiAuditMapper auditMapper;

    // 重点：使用 @Async 异步入库，不影响 AI 给用户吐字的速度
    @Async
    public void saveAuditLog(AiAuditLog log) {
        // 成本计算公式：(总Token / 1,000,000) * 单价
        // 假设 DeepSeek 价格为 1元 / 1M tokens
        double cost = (log.getTotalTokens() / 1000000.0) * 1.0;
        log.setCost(cost);

        auditMapper.insert(log);
    }
}