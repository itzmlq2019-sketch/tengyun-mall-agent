package com.tengyun.order.service.impl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tengyun.order.config.RabbitConfig;
import com.tengyun.order.dto.OrderMessageDTO;
import com.tengyun.order.entity.Order;
import com.tengyun.order.mapper.OrderMapper;
import com.tengyun.order.service.OrderService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private RabbitTemplate rabbitTemplate; // 注入兔子的发信器

    @Override
    public String checkout(Long userId, Long productId, Integer quantity) {
        System.out.println("🟢 [主线程] 接收到下单请求：用户 " + userId + "，商品 " + productId + "，数量 " + quantity);

        // 封装成对象
        OrderMessageDTO messageDTO = new OrderMessageDTO(userId, productId, quantity);

        // 发送整个对象给 MQ
        rabbitTemplate.convertAndSend(RabbitConfig.ORDER_EXCHANGE, RabbitConfig.ORDER_ROUTING_KEY, messageDTO);

        return "您的订单已受理，系统正在为您处理商品 ID 为 " + productId + " 的出库流程！";
    }
    @Override
    public List<Order> getHistory(Long userId) {
        // 核心逻辑：查询该 userId 下的所有订单，并按照 id 倒序排列（最新的订单在最前面）
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).orderByDesc("id");

        return this.list(wrapper);
    }
}