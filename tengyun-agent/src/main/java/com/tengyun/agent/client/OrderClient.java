package com.tengyun.agent.client;

import com.tengyun.agent.dto.CheckoutDTO; // 🌟 修复：引入刚刚在 agent 本地创建的 DTO
import com.tengyun.agent.config.AgentToolConfig.OrderHistoryDTO; // 🌟 引入内部 Record
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "order-service")
public interface OrderClient {

    @PostMapping("/order/checkout")
    String checkout(@RequestBody CheckoutDTO dto);

    // 新增：调用 order-service 的历史订单接口
    @GetMapping("/order/history/{userId}")
    List<OrderHistoryDTO> getHistory(@PathVariable("userId") Long userId);
}