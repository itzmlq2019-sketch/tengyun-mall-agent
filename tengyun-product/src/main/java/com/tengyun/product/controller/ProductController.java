package com.tengyun.product.controller;

import com.tengyun.product.entity.Product;
import com.tengyun.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
 @RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService; // 注入业务层

    // 提供给 Agent 查详情的接口
    @GetMapping("/info/{id}")
    public Product getProductInfo(@PathVariable("id") Long id) {
        return productService.getProductInfo(id);
    }

    // 提供给 Order 微服务扣库存的接口
    @PostMapping("/deduct")
    public String deductStock(@RequestParam("productId") Long productId, @RequestParam("num") Integer num) {
        return productService.deductStock(productId, num);
    }
    @GetMapping("/category/{categoryId}/exclude/{productId}")
    public List<Product> getRelatedProducts(@PathVariable("categoryId") Long categoryId,
                                            @PathVariable("productId") Long productId) {
        return productService.getRelatedProducts(categoryId, productId);
    }
}