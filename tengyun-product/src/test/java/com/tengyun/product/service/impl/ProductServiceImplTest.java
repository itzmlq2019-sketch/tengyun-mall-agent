package com.tengyun.product.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tengyun.product.entity.Product;
import com.tengyun.product.entity.StockDeduction;
import com.tengyun.product.mapper.ProductMapper;
import com.tengyun.product.mapper.StockDeductionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;
    @Mock
    private StockDeductionMapper stockDeductionMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private ProductServiceImpl productService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        productService = new ProductServiceImpl();
        ReflectionTestUtils.setField(productService, "productMapper", productMapper);
        ReflectionTestUtils.setField(productService, "stockDeductionMapper", stockDeductionMapper);
        ReflectionTestUtils.setField(productService, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(productService, "objectMapper", objectMapper);
    }

    @Test
    void shouldReturnProductFromCacheWhenCacheHit() throws JsonProcessingException {
        Product product = product(1L, "Tea", "8.80", 20, 9L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("product::1")).thenReturn(objectMapper.writeValueAsString(product));

        Product result = productService.getProductInfo(1L);

        assertNotNull(result);
        assertEquals("Tea", result.getName());
        verify(productMapper, never()).selectById(any());
    }

    @Test
    void shouldLoadFromDbAndCacheWhenCacheMiss() throws JsonProcessingException {
        Product product = product(2L, "Coffee", "12.50", 50, 10L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("product::2")).thenReturn(null);
        when(productMapper.selectById(2L)).thenReturn(product);

        Product result = productService.getProductInfo(2L);

        assertNotNull(result);
        assertEquals("Coffee", result.getName());
        verify(valueOperations).set(eq("product::2"), eq(objectMapper.writeValueAsString(product)), eq(30L), eq(TimeUnit.MINUTES));
    }

    @Test
    void shouldFallbackToDbWhenCacheReadFails() {
        Product product = product(3L, "Cola", "6.00", 100, 11L);
        when(stringRedisTemplate.opsForValue()).thenThrow(new RuntimeException("REDIS_DOWN"));
        when(productMapper.selectById(3L)).thenReturn(product);

        Product result = productService.getProductInfo(3L);

        assertNotNull(result);
        assertEquals("Cola", result.getName());
        verify(productMapper).selectById(3L);
    }

    @Test
    void shouldDeductStockAndInvalidateCacheWhenSuccess() {
        when(stockDeductionMapper.selectByRequestId("req-2")).thenReturn(null);
        when(productMapper.deductStock(2L, 3)).thenReturn(1);

        String result = productService.deductStock("req-2", 2L, 3);

        assertEquals("SUCCESS", result);
        verify(stringRedisTemplate).delete("product::2");
        verify(productMapper).deductStock(2L, 3);
        verify(stockDeductionMapper).insert(any(StockDeduction.class));
    }

    @Test
    void shouldReturnFailWhenNoRowsAffected() {
        when(stockDeductionMapper.selectByRequestId("req-3")).thenReturn(null);
        when(productMapper.deductStock(2L, 999)).thenReturn(0);

        String result = productService.deductStock("req-3", 2L, 999);

        assertEquals("FAIL", result);
        verify(stringRedisTemplate).delete("product::2");
        verify(productMapper).deductStock(2L, 999);
    }

    @Test
    void shouldNotDeductTwiceWhenRequestAlreadyDeducted() {
        StockDeduction existing = deduction("req-4", 2L, 3, "DEDUCTED");
        when(stockDeductionMapper.selectByRequestId("req-4")).thenReturn(existing);
        when(stockDeductionMapper.selectByRequestIdForUpdate("req-4")).thenReturn(existing);

        String result = productService.deductStock("req-4", 2L, 3);

        assertEquals("SUCCESS", result);
        verify(productMapper, never()).deductStock(any(), any());
    }

    @Test
    void shouldRestoreStockOnlyOnceWhenCompensating() {
        StockDeduction existing = deduction("req-5", 2L, 3, "DEDUCTED");
        when(stockDeductionMapper.selectByRequestIdForUpdate("req-5")).thenReturn(existing);
        when(productMapper.restoreStock(2L, 3)).thenReturn(1);

        String result = productService.compensateStock("req-5");

        assertEquals("SUCCESS", result);
        assertEquals("COMPENSATED", existing.getStatus());
        verify(productMapper).restoreStock(2L, 3);
        verify(stockDeductionMapper).updateById(existing);
    }

    @Test
    void shouldReturnNullWhenDbAlsoMisses() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("product::999")).thenReturn(null);
        when(productMapper.selectById(999L)).thenReturn(null);

        Product result = productService.getProductInfo(999L);

        assertNull(result);
        verify(valueOperations, never()).set(any(), any(), any(Long.class), any(TimeUnit.class));
    }

    private Product product(Long id, String name, String price, Integer stock, Long categoryId) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setStock(stock);
        product.setCategoryId(categoryId);
        return product;
    }

    private StockDeduction deduction(String requestId, Long productId, Integer quantity, String status) {
        StockDeduction deduction = new StockDeduction();
        deduction.setId(1L);
        deduction.setRequestId(requestId);
        deduction.setProductId(productId);
        deduction.setQuantity(quantity);
        deduction.setStatus(status);
        return deduction;
    }
}
