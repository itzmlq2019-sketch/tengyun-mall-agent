package com.tengyun.product.mapper;
import org.apache.ibatis.annotations.Update;
import com.tengyun.order.dto.ProductDTO;
import com.tengyun.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
// 记得导入你的 ProductDTO

@Mapper
public interface ProductMapper {

    // 1. 根据商品 ID 查出它的分类 ID
    @Select("SELECT category_id FROM product WHERE id = #{productId}")
    Long getCategoryIdByProductId(@Param("productId") Long productId);
    // 🌟 根据 ID 查商品详情
    @Select("SELECT * FROM product WHERE id = #{id}")
    Product selectById(Long id);

    // 2. 查同分类的其他商品（排除自己，且状态为起售 1），限制返回 2 条防 AI 话痨
    @Select("SELECT * FROM product WHERE category_id = #{categoryId} AND id != #{productId} AND status = 1 LIMIT 2")
    List<Product> getByCategoryIdAndNotId(@Param("categoryId") Long categoryId, @Param("productId") Long productId);
    // 返回值 int 表示受影响的行数。如果扣减成功返回 1，库存不足扣减失败返回 0。
    @Update("UPDATE product SET stock = stock - #{num} WHERE id = #{productId} AND stock >= #{num}")
    int deductStock(@Param("productId") Long productId, @Param("num") Integer num);
}