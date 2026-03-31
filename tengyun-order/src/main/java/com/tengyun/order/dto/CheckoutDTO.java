package com.tengyun.order.dto;

import lombok.Data;

@Data
public class CheckoutDTO {
    private Long userId;
    private Long productId;
    private Integer quantity;
}