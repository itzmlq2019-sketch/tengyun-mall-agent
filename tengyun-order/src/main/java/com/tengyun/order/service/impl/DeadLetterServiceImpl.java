package com.tengyun.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tengyun.order.config.RabbitConfig;
import com.tengyun.order.dto.OrderMessageDTO;
import com.tengyun.order.entity.OrderDeadLetterLog;
import com.tengyun.order.mapper.OrderDeadLetterLogMapper;
import com.tengyun.order.service.DeadLetterService;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeadLetterServiceImpl implements DeadLetterService {

    private final OrderDeadLetterLogMapper deadLetterLogMapper;
    private final RabbitTemplate rabbitTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public DeadLetterServiceImpl(
            OrderDeadLetterLogMapper deadLetterLogMapper,
            RabbitTemplate rabbitTemplate,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            StringRedisTemplate stringRedisTemplate
    ) {
        this.deadLetterLogMapper = deadLetterLogMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public List<OrderDeadLetterLog> latest(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        QueryWrapper<OrderDeadLetterLog> qw = new QueryWrapper<>();
        qw.orderByDesc("id").last("LIMIT " + safeLimit);
        return deadLetterLogMapper.selectList(qw);
    }

    @Override
    public String requeue(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("INVALID_ID");
        }
        OrderDeadLetterLog log = deadLetterLogMapper.selectById(id);
        if (log == null || log.getPayload() == null || log.getPayload().isBlank()) {
            throw new IllegalArgumentException("DEAD_LETTER_NOT_FOUND");
        }
        try {
            OrderMessageDTO dto = objectMapper.readValue(log.getPayload(), OrderMessageDTO.class);
            if (dto.getRequestId() != null && !dto.getRequestId().isBlank()) {
                stringRedisTemplate.delete("order:idempotent:" + dto.getRequestId());
            }
            rabbitTemplate.convertAndSend(
                    RabbitConfig.ORDER_EXCHANGE,
                    RabbitConfig.ORDER_ROUTING_KEY,
                    dto,
                    message -> {
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        return message;
                    }
            );
            return "REQUEUED";
        } catch (Exception e) {
            throw new IllegalArgumentException("INVALID_DEAD_LETTER_PAYLOAD");
        }
    }
}
