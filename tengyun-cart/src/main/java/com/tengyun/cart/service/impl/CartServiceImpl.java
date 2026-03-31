package com.tengyun.cart.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tengyun.cart.entity.CartItem;
import com.tengyun.cart.mapper.CartItemMapper;
import com.tengyun.cart.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartItemMapper cartItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class) // 涉及写操作，加上事务兜底
    public void addCart(Long userId, Long productId, String productName, BigDecimal price) {
        // 这里如果是大厂逻辑，通常会先查一下该商品是否已在购物车，有则 quantity+1，没有则 insert
        // 先保留原来的逻辑，做纯粹的重构
        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setProductId(productId);
        item.setProductName(productName);
        item.setPrice(price);
        item.setQuantity(1);

        cartItemMapper.insert(item);
    }

    @Override
    public List<CartItem> getMyCart(Long userId) {
        QueryWrapper<CartItem> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        return cartItemMapper.selectList(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearCart(Long userId) {
        QueryWrapper<CartItem> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        cartItemMapper.delete(wrapper);
    }
}