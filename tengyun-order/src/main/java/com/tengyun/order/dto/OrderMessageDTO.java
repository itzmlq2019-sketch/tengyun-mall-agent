package com.tengyun.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderMessageDTO implements Serializable {
    private Long userId;
    private Long productId;
    private Integer quantity;
}
