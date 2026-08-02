package com.tengyun.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CheckoutDTO {
    private Long userId;
    @NotNull(message = "PRODUCT_ID_REQUIRED")
    @Min(value = 1, message = "PRODUCT_ID_INVALID")
    private Long productId;
    @NotNull(message = "QUANTITY_REQUIRED")
    @Min(value = 1, message = "QUANTITY_INVALID")
    private Integer quantity;
}
