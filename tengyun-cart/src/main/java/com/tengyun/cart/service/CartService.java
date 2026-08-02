package com.tengyun.cart.service;

import com.tengyun.cart.entity.CartItem;

import java.math.BigDecimal;
import java.util.List;

public interface CartService {

    void addCart(Long userId, Long productId, String productName, BigDecimal price, Integer quantity);

    List<CartItem> getMyCart(Long userId);

    void removeItem(Long userId, Long productId);

    void clearCart(Long userId);
}
