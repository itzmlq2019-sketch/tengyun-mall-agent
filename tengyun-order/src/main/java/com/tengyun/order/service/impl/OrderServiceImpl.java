package com.tengyun.order.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tengyun.order.config.RabbitConfig;
import com.tengyun.order.dto.OrderMessageDTO;
import com.tengyun.order.entity.Order;
import com.tengyun.order.mapper.OrderMapper;
import com.tengyun.order.service.OrderService;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private static final long PUBLISH_CONFIRM_TIMEOUT_SECONDS = 5;

    @Autowired
    private RabbitTemplate rabbitTemplate; // 注入兔子的发信器

    @Override
    public String checkout(Long userId, Long productId, Integer quantity) {
        if (userId == null || productId == null || quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("INVALID_ORDER_REQUEST");
        }
        String requestId = UUID.randomUUID().toString();

        OrderMessageDTO messageDTO = new OrderMessageDTO(requestId, userId, productId, quantity);

        CorrelationData correlationData = new CorrelationData(requestId);
        rabbitTemplate.convertAndSend(
                RabbitConfig.ORDER_EXCHANGE,
                RabbitConfig.ORDER_ROUTING_KEY,
                messageDTO,
                message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return message;
                },
                correlationData
        );

        awaitBrokerConfirmation(correlationData);

        return "ORDER_ACCEPTED:" + requestId;
    }

    private void awaitBrokerConfirmation(CorrelationData correlationData) {
        try {
            CorrelationData.Confirm confirm = correlationData.getFuture()
                    .get(PUBLISH_CONFIRM_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!confirm.isAck()) {
                throw new IllegalStateException("ORDER_MESSAGE_NACK:" + confirm.getReason());
            }
            if (correlationData.getReturned() != null) {
                throw new IllegalStateException("ORDER_MESSAGE_UNROUTABLE");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("ORDER_MESSAGE_CONFIRM_INTERRUPTED", e);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new IllegalStateException("ORDER_MESSAGE_CONFIRM_TIMEOUT", e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new IllegalStateException("ORDER_MESSAGE_CONFIRM_FAILED", e);
        }
    }
    @Override
    public List<Order> getHistory(Long userId) {
        // 核心逻辑：查询该 userId 下的所有订单，并按照 id 倒序排列（最新的订单在最前面）
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("id");

        return this.list(wrapper);
    }
}
