package com.tengyun.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tengyun.order.entity.Order;

import java.util.List;

public interface OrderService {
    // 增加参数：productId 和 quantity
    String checkout(Long userId, Long productId, Integer quantity);
    List<Order> getHistory(Long userId);
}
