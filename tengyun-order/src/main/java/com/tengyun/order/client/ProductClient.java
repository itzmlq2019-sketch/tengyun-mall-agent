package com.tengyun.order.client;
import com.tengyun.order.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
@FeignClient(name = "product-service")
public interface ProductClient {
    @GetMapping("/product/info/{id}")
    ProductDTO getProductInfo(@PathVariable("id") Long id);
    @PostMapping("/product/deduct")
    String deductStock(@RequestParam("productId") Long productId, @RequestParam("num") Integer num);
}