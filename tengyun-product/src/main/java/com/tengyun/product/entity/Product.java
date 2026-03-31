package com.tengyun.product.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.io.Serializable;
@Data
@TableName("product")
public class Product implements Serializable{
    // 加上这个序列化版本号是一个良好的 Java 编程习惯
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stock;
    private Long categoryId;
}