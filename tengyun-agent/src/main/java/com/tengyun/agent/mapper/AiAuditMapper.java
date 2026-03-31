package com.tengyun.agent.mapper;

import com.tengyun.agent.entity.AiAuditLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiAuditMapper {
    // 这里使用最简单的注解式 MyBatis，或者用 MyBatis-Plus
    @Insert("INSERT INTO ai_audit_log (user_id, user_message, ai_response, model, prompt_tokens, completion_tokens, total_tokens, cost, latency, create_time) " +
            "VALUES (#{userId}, #{userMessage}, #{aiResponse}, #{model}, #{promptTokens}, #{completionTokens}, #{totalTokens}, #{cost}, #{latency}, NOW())")
    void insert(AiAuditLog log);
}