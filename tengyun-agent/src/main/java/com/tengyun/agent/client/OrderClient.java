package com.tengyun.agent.client;

import com.tengyun.agent.config.AgentToolConfig.OrderHistoryDTO;
import com.tengyun.agent.dto.ApiResponse;
import com.tengyun.agent.dto.CheckoutDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "order-service")
public interface OrderClient {

    @PostMapping("/order/checkout")
    ApiResponse<String> checkout(@RequestHeader("X-User-Id") Long userId, @RequestBody CheckoutDTO dto);

    @GetMapping("/order/history")
    ApiResponse<List<OrderHistoryDTO>> getHistory(@RequestHeader("X-User-Id") Long userId);
}
