package com.tengyun.order.controller;

import com.tengyun.order.dto.ApiResponse;
import com.tengyun.order.dto.CheckoutDTO;
import com.tengyun.order.entity.OrderDeadLetterLog;
import com.tengyun.order.entity.Order;
import com.tengyun.order.service.DeadLetterService;
import com.tengyun.order.service.OrderService;
import com.tengyun.order.security.AdminApiGuard;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@RestController
@Validated
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;
    private final DeadLetterService deadLetterService;
    private final AdminApiGuard adminApiGuard;

    public OrderController(OrderService orderService, DeadLetterService deadLetterService, AdminApiGuard adminApiGuard) {
        this.orderService = orderService;
        this.deadLetterService = deadLetterService;
        this.adminApiGuard = adminApiGuard;
    }

    @PostMapping("/checkout")
    public ApiResponse<String> checkout(
            @RequestHeader("X-User-Id") @Min(value = 1, message = "USER_ID_INVALID") Long userId,
            @Valid @RequestBody CheckoutDTO dto
    ) {
        return ApiResponse.success(orderService.checkout(userId, dto.getProductId(), dto.getQuantity()));
    }

    @GetMapping("/history")
    public ApiResponse<List<Order>> getHistory(
            @RequestHeader("X-User-Id") @Min(value = 1, message = "USER_ID_INVALID") Long userId
    ) {
        return ApiResponse.success(orderService.getHistory(userId));
    }

    @GetMapping("/dead-letter")
    public ApiResponse<List<OrderDeadLetterLog>> deadLetters(
            @RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "LIMIT_INVALID") @Max(value = 100, message = "LIMIT_TOO_LARGE") Integer limit
    ) {
        adminApiGuard.verify(adminToken);
        return ApiResponse.success(deadLetterService.latest(limit));
    }

    @PostMapping("/dead-letter/requeue")
    public ApiResponse<String> requeue(
            @RequestHeader(value = "X-Admin-Token", required = false) String adminToken,
            @RequestParam("id") @Min(value = 1, message = "ID_INVALID") Long id
    ) {
        adminApiGuard.verify(adminToken);
        return ApiResponse.success(deadLetterService.requeue(id));
    }
}
