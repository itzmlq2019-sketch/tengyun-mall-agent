package com.tengyun.order.controller;

import com.tengyun.order.entity.Order;
import com.tengyun.order.dto.CheckoutDTO;
import com.tengyun.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService; // 注入业务层
    // 在 order-service 的 OrderController 中
    @PostMapping("/checkout")
    public String checkout(@RequestBody CheckoutDTO dto) { // 改为接收 JSON 对象
        return orderService.checkout(dto.getUserId(), dto.getProductId(), dto.getQuantity());
    }
    @GetMapping("/history/{userId}")
    public List<Order> getHistory(@PathVariable("userId") Long userId) {
        System.out.println("🟢 [Order服务] 收到历史订单查询请求，用户ID: " + userId);
        return orderService.getHistory(userId);
    }
}