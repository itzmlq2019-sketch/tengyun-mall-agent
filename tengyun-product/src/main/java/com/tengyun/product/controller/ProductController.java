package com.tengyun.product.controller;

import com.tengyun.product.dto.ApiResponse;
import com.tengyun.product.entity.Product;
import com.tengyun.product.service.ProductService;
import com.tengyun.product.security.InternalApiGuard;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/product")
public class ProductController {

    private final ProductService productService;
    private final InternalApiGuard internalApiGuard;

    public ProductController(ProductService productService, InternalApiGuard internalApiGuard) {
        this.productService = productService;
        this.internalApiGuard = internalApiGuard;
    }

    @GetMapping("/info/{id}")
    public ApiResponse<Product> getProductInfo(
            @PathVariable("id") @Min(value = 1, message = "PRODUCT_ID_INVALID") Long id
    ) {
        return ApiResponse.success(productService.getProductInfo(id));
    }

    @PostMapping("/deduct")
    public ApiResponse<String> deductStock(
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken,
            @RequestParam("requestId") @NotNull(message = "REQUEST_ID_REQUIRED") String requestId,
            @RequestParam("productId") @NotNull(message = "PRODUCT_ID_REQUIRED") @Min(value = 1, message = "PRODUCT_ID_INVALID") Long productId,
            @RequestParam("num") @NotNull(message = "QUANTITY_REQUIRED") @Min(value = 1, message = "QUANTITY_INVALID") Integer num
    ) {
        internalApiGuard.verify(internalToken);
        return ApiResponse.success(productService.deductStock(requestId, productId, num));
    }

    @PostMapping("/deduct/compensate")
    public ApiResponse<String> compensateStock(
            @RequestHeader(value = "X-Internal-Token", required = false) String internalToken,
            @RequestParam("requestId") @NotNull(message = "REQUEST_ID_REQUIRED") String requestId
    ) {
        internalApiGuard.verify(internalToken);
        return ApiResponse.success(productService.compensateStock(requestId));
    }

    @GetMapping("/category/{categoryId}/exclude/{productId}")
    public ApiResponse<List<Product>> getRelatedProducts(
            @PathVariable("categoryId") @Min(value = 1, message = "CATEGORY_ID_INVALID") Long categoryId,
            @PathVariable("productId") @Min(value = 1, message = "PRODUCT_ID_INVALID") Long productId
    ) {
        return ApiResponse.success(productService.getRelatedProducts(categoryId, productId));
    }
}
