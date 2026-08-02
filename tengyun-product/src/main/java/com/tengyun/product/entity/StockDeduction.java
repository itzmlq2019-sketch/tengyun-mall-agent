package com.tengyun.product.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("product_stock_deduction")
public class StockDeduction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String requestId;
    private Long productId;
    private Integer quantity;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
