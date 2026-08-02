package com.tengyun.product.mapper;

import com.tengyun.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProductMapper {

    @Select("SELECT category_id FROM product WHERE id = #{productId}")
    Long getCategoryIdByProductId(@Param("productId") Long productId);

    @Select("SELECT * FROM product WHERE id = #{id}")
    Product selectById(Long id);

    @Select("SELECT * FROM product WHERE category_id = #{categoryId} AND id != #{productId} AND status = 1 LIMIT 2")
    List<Product> getByCategoryIdAndNotId(@Param("categoryId") Long categoryId, @Param("productId") Long productId);

    @Update("UPDATE product SET stock = stock - #{num} WHERE id = #{productId} AND stock >= #{num}")
    int deductStock(@Param("productId") Long productId, @Param("num") Integer num);

    @Update("UPDATE product SET stock = stock + #{num} WHERE id = #{productId}")
    int restoreStock(@Param("productId") Long productId, @Param("num") Integer num);
}
