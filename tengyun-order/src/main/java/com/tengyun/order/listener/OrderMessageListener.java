package com.tengyun.order.listener;

import com.tengyun.order.client.CartClient;
import com.tengyun.order.client.ProductClient;
import com.tengyun.order.config.RabbitConfig;
import com.tengyun.order.dto.ApiResponse;
import com.tengyun.order.dto.OrderMessageDTO;
import com.tengyun.order.dto.ProductDTO;
import com.tengyun.order.entity.Order;
import com.tengyun.order.mapper.OrderMapper;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class OrderMessageListener {

    private final RedissonClient redissonClient;
    private final ProductClient productClient;
    private final CartClient cartClient;
    private final OrderMapper orderMapper;

    public OrderMessageListener(
            RedissonClient redissonClient,
            ProductClient productClient,
            CartClient cartClient,
            OrderMapper orderMapper
    ) {
        this.redissonClient = redissonClient;
        this.productClient = productClient;
        this.cartClient = cartClient;
        this.orderMapper = orderMapper;
    }

    @RabbitListener(queues = RabbitConfig.ORDER_QUEUE)
    public void handleOrderMessage(OrderMessageDTO dto) {
        validateMessage(dto);

        Long userId = dto.getUserId();
        Long productId = dto.getProductId();
        Integer quantity = dto.getQuantity();

        String lockKey = "lock:product:stock:" + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(3, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new RuntimeException("LOCK_BUSY");
            }

            Order existingOrder = orderMapper.selectByRequestId(dto.getRequestId());
            if (existingOrder != null) {
                cartClient.removeItem(existingOrder.getUserId(), existingOrder.getProductId());
                return;
            }

            ApiResponse<ProductDTO> productResp = productClient.getProductInfo(productId);
            ProductDTO product = productResp == null ? null : productResp.data();
            if (product == null) {
                throw new AmqpRejectAndDontRequeueException("PRODUCT_NOT_FOUND");
            }

            ApiResponse<String> deductResp = productClient.deductStock(dto.getRequestId(), productId, quantity);
            String result = deductResp == null ? null : deductResp.data();
            if (!"SUCCESS".equals(result)) {
                throw new RuntimeException("DEDUCT_STOCK_FAILED");
            }

            Order order = new Order();
            order.setRequestId(dto.getRequestId());
            order.setUserId(userId);
            order.setProductId(productId);
            order.setProductName(product.getName());
            order.setPrice(product.getPrice());
            order.setQuantity(quantity);
            order.setStatus("CREATED");
            order.setTotalAmount(product.getPrice().multiply(new java.math.BigDecimal(quantity)));
            try {
                orderMapper.insert(order);
            } catch (Exception insertException) {
                Order committedOrder = orderMapper.selectByRequestId(dto.getRequestId());
                if (committedOrder == null) {
                    compensateStock(dto.getRequestId(), insertException);
                }
                throw insertException;
            }

            cartClient.removeItem(userId, productId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("THREAD_INTERRUPTED", e);
        } catch (AmqpRejectAndDontRequeueException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("ORDER_CONSUME_FAILED", e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void validateMessage(OrderMessageDTO dto) {
        if (dto == null || dto.getRequestId() == null || dto.getRequestId().isBlank()
                || dto.getUserId() == null || dto.getProductId() == null || dto.getQuantity() == null) {
            throw new AmqpRejectAndDontRequeueException("INVALID_ORDER_MESSAGE");
        }
        if (dto.getQuantity() <= 0) {
            throw new AmqpRejectAndDontRequeueException("INVALID_QUANTITY");
        }
    }

    private void compensateStock(String requestId, Exception originalException) {
        try {
            ApiResponse<String> response = productClient.compensateStock(requestId);
            if (response == null || !"SUCCESS".equals(response.data())) {
                originalException.addSuppressed(new IllegalStateException("STOCK_COMPENSATION_FAILED"));
            }
        } catch (Exception compensationException) {
            originalException.addSuppressed(compensationException);
        }
    }
}
