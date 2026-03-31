package com.tengyun.agent.client;

import com.tengyun.agent.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;

@FeignClient(name = "product-service")
public interface ProductClient {


    @GetMapping("/product/info/{id}")
    ProductDTO getProductInfo(@PathVariable("id") Long id);

    //推荐接口
    @GetMapping("/product/category/{categoryId}/exclude/{productId}")
    List<ProductDTO> getRelatedProducts(@PathVariable("categoryId") Long categoryId,
                                        @PathVariable("productId") Long productId);
}