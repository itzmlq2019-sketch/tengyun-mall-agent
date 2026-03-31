package com.tengyun.order.client;
import com.tengyun.order.dto.CartDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.List;

@FeignClient(name = "cart-service")
public interface CartClient {

    // 远程获取购物车列表
    @GetMapping("/cart/list")
    List<CartDTO> myCart(@RequestHeader("X-User-Id") Long userId);

    // 远程清空购物车
    @DeleteMapping("/cart/clear")
    String clearCart(@RequestHeader("X-User-Id") Long userId);
}