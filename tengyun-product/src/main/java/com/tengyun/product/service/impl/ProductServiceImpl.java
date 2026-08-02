package com.tengyun.product.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tengyun.product.entity.Product;
import com.tengyun.product.entity.StockDeduction;
import com.tengyun.product.mapper.ProductMapper;
import com.tengyun.product.mapper.StockDeductionMapper;
import com.tengyun.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StockDeductionMapper stockDeductionMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Product getProductInfo(Long id) {
        String cacheKey = "product::" + id;
        try {
            String productJson = stringRedisTemplate.opsForValue().get(cacheKey);
            if (productJson != null && !productJson.isEmpty()) {
                return objectMapper.readValue(productJson, Product.class);
            }

            Product product = productMapper.selectById(id);
            if (product != null) {
                stringRedisTemplate.opsForValue().set(
                        cacheKey,
                        objectMapper.writeValueAsString(product),
                        30,
                        TimeUnit.MINUTES
                );
            }
            return product;
        } catch (Exception e) {
            return productMapper.selectById(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String deductStock(String requestId, Long productId, Integer num) {
        validateStockRequest(requestId, productId, num);
        StockDeduction existing = stockDeductionMapper.selectByRequestId(requestId);
        if (existing != null) {
            existing = stockDeductionMapper.selectByRequestIdForUpdate(requestId);
            if (!productId.equals(existing.getProductId()) || !num.equals(existing.getQuantity())) {
                return "CONFLICT";
            }
            if ("DEDUCTED".equals(existing.getStatus())) {
                return "SUCCESS";
            }
        }

        String cacheKey = "product::" + productId;
        stringRedisTemplate.delete(cacheKey);

        int rows = productMapper.deductStock(productId, num);
        if (rows <= 0) {
            return "FAIL";
        }

        if (existing == null) {
            StockDeduction deduction = new StockDeduction();
            deduction.setRequestId(requestId);
            deduction.setProductId(productId);
            deduction.setQuantity(num);
            deduction.setStatus("DEDUCTED");
            stockDeductionMapper.insert(deduction);
        } else {
            existing.setStatus("DEDUCTED");
            stockDeductionMapper.updateById(existing);
        }

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(500);
                stringRedisTemplate.delete(cacheKey);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        return "SUCCESS";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String compensateStock(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("REQUEST_ID_REQUIRED");
        }
        StockDeduction deduction = stockDeductionMapper.selectByRequestIdForUpdate(requestId);
        if (deduction == null) {
            return "NOT_FOUND";
        }
        if ("COMPENSATED".equals(deduction.getStatus())) {
            return "SUCCESS";
        }
        int rows = productMapper.restoreStock(deduction.getProductId(), deduction.getQuantity());
        if (rows <= 0) {
            throw new IllegalStateException("RESTORE_STOCK_FAILED");
        }
        deduction.setStatus("COMPENSATED");
        stockDeductionMapper.updateById(deduction);
        stringRedisTemplate.delete("product::" + deduction.getProductId());
        return "SUCCESS";
    }

    private void validateStockRequest(String requestId, Long productId, Integer num) {
        if (requestId == null || requestId.isBlank()) {
            throw new IllegalArgumentException("REQUEST_ID_REQUIRED");
        }
        if (productId == null || productId <= 0 || num == null || num <= 0) {
            throw new IllegalArgumentException("INVALID_STOCK_REQUEST");
        }
    }

    @Override
    public List<Product> getRelatedProducts(Long categoryId, Long productId) {
        return productMapper.getByCategoryIdAndNotId(categoryId, productId);
    }
}
