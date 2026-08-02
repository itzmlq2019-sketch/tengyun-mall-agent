package com.tengyun.order.client;

import com.tengyun.order.dto.ApiResponse;
import com.tengyun.order.dto.CartDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "cart-service")
public interface CartClient {

    @GetMapping("/cart/list")
    ApiResponse<List<CartDTO>> myCart(@RequestHeader("X-User-Id") Long userId);

    @DeleteMapping("/cart/item")
    ApiResponse<String> removeItem(@RequestHeader("X-User-Id") Long userId,
                                   @RequestParam("productId") Long productId);

    @DeleteMapping("/cart/clear")
    ApiResponse<String> clearCart(@RequestHeader("X-User-Id") Long userId);
}
