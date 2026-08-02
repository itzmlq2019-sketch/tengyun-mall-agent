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
    @Transactional(rollbackFor = Exception.class)
    public void addCart(Long userId, Long productId, String productName, BigDecimal price, Integer quantity) {
        int safeQuantity = (quantity == null || quantity <= 0) ? 1 : quantity;

        QueryWrapper<CartItem> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("product_id", productId);
        CartItem existingItem = cartItemMapper.selectOne(wrapper);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + safeQuantity);
            existingItem.setProductName(productName);
            existingItem.setPrice(price);
            cartItemMapper.updateById(existingItem);
            return;
        }

        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setProductId(productId);
        item.setProductName(productName);
        item.setPrice(price);
        item.setQuantity(safeQuantity);
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
    public void removeItem(Long userId, Long productId) {
        QueryWrapper<CartItem> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId).eq("product_id", productId);
        cartItemMapper.delete(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearCart(Long userId) {
        QueryWrapper<CartItem> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        cartItemMapper.delete(wrapper);
    }
}
