package com.tengyun.order.listener;

import com.tengyun.order.client.ProductClient;
import com.tengyun.order.config.RabbitConfig;
import com.tengyun.order.dto.OrderMessageDTO;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Component
public class OrderMessageListener {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private ProductClient productClient;

    @RabbitListener(queues = RabbitConfig.ORDER_QUEUE)
    public void handleOrderMessage(OrderMessageDTO dto) { // 接收对象
        Long userId = dto.getUserId();
        Long productId = dto.getProductId();
        Integer quantity = dto.getQuantity();

        System.out.println(" [MQ消费者] 准备处理：用户 " + userId + " 购买商品 " + productId + " 共 " + quantity + " 件");

        // 动态加锁：只锁当前这款商品，不影响别的商品下单
        String lockKey = "lock:product:stock:" + productId;
        RLock lock = redissonClient.getLock(lockKey);

        try {
            boolean isLocked = lock.tryLock(3, TimeUnit.SECONDS);
            if (!isLocked) return;

            // 动态扣减：把真实的 productId 和 quantity 传给 Feign 接口
            String result = productClient.deductStock(productId, quantity);

            if ("SUCCESS".equals(result)) {
                System.out.println("✅ [MQ消费者] 商品 " + productId + " 扣减成功！");
            } else {
                System.out.println("❌ [MQ消费者] 库存不足，扣减失败！");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }
}