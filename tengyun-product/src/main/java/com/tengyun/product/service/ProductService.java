package com.tengyun.product.service;

import com.tengyun.product.entity.Product;
import java.util.List;
public interface ProductService {
    // 1. 查询商品详情
    Product getProductInfo(Long id);

    // 2. 扣减库存（包含双删逻辑）
    String deductStock(Long productId, Integer num);
    List<Product> getRelatedProducts(Long categoryId, Long productId);
    // (如果之前还有查推荐商品的方法，也统一定义在这里)
}