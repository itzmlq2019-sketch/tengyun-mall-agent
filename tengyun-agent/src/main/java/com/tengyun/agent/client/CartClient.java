package com.tengyun.agent.client;

import com.tengyun.agent.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "cart-service")
public interface CartClient {

    @PostMapping("/cart/add")
    ApiResponse<String> addCart(@RequestHeader("X-User-Id") Long userId,
                                @RequestParam("productId") Long productId,
                                @RequestParam("productName") String productName,
                                @RequestParam("price") String price,
                                @RequestParam("quantity") Integer quantity);
}
