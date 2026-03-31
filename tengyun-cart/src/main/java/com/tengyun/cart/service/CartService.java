package com.tengyun.cart.service;

import com.tengyun.cart.entity.CartItem;
import java.math.BigDecimal;
import java.util.List;

public interface CartService {
    /**
     * 加入购物车
     */
    void addCart(Long userId, Long productId, String productName, BigDecimal price);

    /**
     * 查看我的购物车
     */
    List<CartItem> getMyCart(Long userId);

    /**
     * 清空购物车
     */
    void clearCart(Long userId);
}