package com.tengyun.order.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tengyun.order.config.RabbitConfig;
import com.tengyun.order.dto.OrderMessageDTO;
import com.tengyun.order.entity.OrderDeadLetterLog;
import com.tengyun.order.mapper.OrderDeadLetterLogMapper;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class OrderDeadLetterListener {

    private final ObjectMapper objectMapper;
    private final OrderDeadLetterLogMapper deadLetterLogMapper;

    public OrderDeadLetterListener(ObjectMapper objectMapper, OrderDeadLetterLogMapper deadLetterLogMapper) {
        this.objectMapper = objectMapper;
        this.deadLetterLogMapper = deadLetterLogMapper;
    }

    @RabbitListener(queues = RabbitConfig.ORDER_DLX_QUEUE)
    public void handleDeadLetter(Message message) {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        String reason = parseReason(message);
        String requestId = parseRequestId(payload);

        OrderDeadLetterLog log = new OrderDeadLetterLog();
        log.setRequestId(requestId);
        log.setReason(reason);
        log.setPayload(payload);
        deadLetterLogMapper.insert(log);
    }

    private String parseRequestId(String payload) {
        try {
            OrderMessageDTO dto = objectMapper.readValue(payload, OrderMessageDTO.class);
            return dto.getRequestId();
        } catch (Exception e) {
            return null;
        }
    }

    private String parseReason(Message message) {
        Object xDeath = message.getMessageProperties().getHeaders().get("x-death");
        if (xDeath instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> map) {
            Object reason = map.get("reason");
            if (reason != null) {
                return reason.toString();
            }
        }
        return "UNKNOWN";
    }
}
