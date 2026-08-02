package com.tengyun.agent.client;

import com.tengyun.agent.dto.ApiResponse;
import com.tengyun.agent.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/product/info/{id}")
    ApiResponse<ProductDTO> getProductInfo(@PathVariable("id") Long id);

    @GetMapping("/product/category/{categoryId}/exclude/{productId}")
    ApiResponse<List<ProductDTO>> getRelatedProducts(@PathVariable("categoryId") Long categoryId,
                                                     @PathVariable("productId") Long productId);
}
