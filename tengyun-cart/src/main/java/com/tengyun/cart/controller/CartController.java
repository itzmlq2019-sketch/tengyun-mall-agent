package com.tengyun.cart.controller;

import com.tengyun.cart.entity.CartItem;
import com.tengyun.cart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @PostMapping("/add")
    public String addCart(@RequestHeader("X-User-Id") Long userId,
                          @RequestParam Long productId,
                          @RequestParam String productName,
                          @RequestParam BigDecimal price) {

        cartService.addCart(userId, productId, productName, price);
        return "商品【" + productName + "】已成功加入购物车！";
    }

    @GetMapping("/list")
    public List<CartItem> myCart(@RequestHeader("X-User-Id") Long userId) {
        return cartService.getMyCart(userId);
    }

    @DeleteMapping("/clear")
    public String clearCart(@RequestHeader("X-User-Id") Long userId) {
        cartService.clearCart(userId);
        return "购物车已清空！";
    }
}