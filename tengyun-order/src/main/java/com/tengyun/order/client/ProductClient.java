package com.tengyun.order.client;
import com.tengyun.order.dto.ApiResponse;
import com.tengyun.order.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
@FeignClient(name = "product-service")
public interface ProductClient {
    @GetMapping("/product/info/{id}")
    ApiResponse<ProductDTO> getProductInfo(@PathVariable("id") Long id);
    @PostMapping("/product/deduct")
    ApiResponse<String> deductStock(@RequestParam("requestId") String requestId,
                                    @RequestParam("productId") Long productId,
                                    @RequestParam("num") Integer num);
    @PostMapping("/product/deduct/compensate")
    ApiResponse<String> compensateStock(@RequestParam("requestId") String requestId);
}
