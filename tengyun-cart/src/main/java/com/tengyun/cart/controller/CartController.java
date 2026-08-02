package com.tengyun.cart.controller;

import com.tengyun.cart.dto.ApiResponse;
import com.tengyun.cart.entity.CartItem;
import com.tengyun.cart.service.CartService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;

@RestController
@Validated
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/add")
    public ApiResponse<String> addCart(
            @RequestHeader("X-User-Id") @Min(value = 1, message = "USER_ID_INVALID") Long userId,
            @RequestParam @NotNull(message = "PRODUCT_ID_REQUIRED") @Min(value = 1, message = "PRODUCT_ID_INVALID") Long productId,
            @RequestParam @NotBlank(message = "PRODUCT_NAME_REQUIRED") String productName,
            @RequestParam @NotNull(message = "PRICE_REQUIRED") BigDecimal price,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "QUANTITY_INVALID") Integer quantity
    ) {
        cartService.addCart(userId, productId, productName, price, quantity);
        return ApiResponse.success("ADD_CART_SUCCESS");
    }

    @GetMapping("/list")
    public ApiResponse<List<CartItem>> myCart(
            @RequestHeader("X-User-Id") @Min(value = 1, message = "USER_ID_INVALID") Long userId
    ) {
        return ApiResponse.success(cartService.getMyCart(userId));
    }

    @DeleteMapping("/item")
    public ApiResponse<String> removeItem(
            @RequestHeader("X-User-Id") @Min(value = 1, message = "USER_ID_INVALID") Long userId,
            @RequestParam @NotNull(message = "PRODUCT_ID_REQUIRED") @Min(value = 1, message = "PRODUCT_ID_INVALID") Long productId
    ) {
        cartService.removeItem(userId, productId);
        return ApiResponse.success("REMOVE_CART_ITEM_SUCCESS");
    }

    @DeleteMapping("/clear")
    public ApiResponse<String> clearCart(
            @RequestHeader("X-User-Id") @Min(value = 1, message = "USER_ID_INVALID") Long userId
    ) {
        cartService.clearCart(userId);
        return ApiResponse.success("CLEAR_CART_SUCCESS");
    }
}
