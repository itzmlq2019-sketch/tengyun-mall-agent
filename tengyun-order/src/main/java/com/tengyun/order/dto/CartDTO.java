package com.tengyun.order.dto;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CartDTO {
    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
}