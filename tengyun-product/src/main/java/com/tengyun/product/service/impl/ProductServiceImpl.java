package com.tengyun.product.service.impl;

import com.tengyun.product.entity.Product;
import com.tengyun.product.mapper.ProductMapper;
import com.tengyun.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import com.fasterxml.jackson.databind.ObjectMapper;
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    // 1. 查询详情（把缓存注解移到 Service 层，这是最标准的大厂做法！）
    @Override
    public Product getProductInfo(Long id) {
        String cacheKey = "product::" + id;

        try {
            // 1. 第一步：先去 Redis 里查（拦截高并发读请求）
            String productJson = stringRedisTemplate.opsForValue().get(cacheKey);

            if (productJson != null && !productJson.isEmpty()) {
                System.out.println("🟢 [Service层] 缓存精准命中！直接返回 Redis 数据！");
                // 使用 Jackson 将 JSON 字符串反序列化为 Java 对象
                return objectMapper.readValue(productJson, Product.class);
            }

            // 2. 第二步：Redis 里没有，老老实实穿透去查 MySQL
            System.out.println("🟢 [Service层] 缓存未命中，正在查询 MySQL 数据库...");
            Product product = productMapper.selectById(id);

            // 3. 第三步：MySQL 查到了数据，把它变回 JSON 塞进 Redis！
            if (product != null) {
                // 使用 Jackson 将 Java 对象序列化为 JSON 字符串
                String jsonStr = objectMapper.writeValueAsString(product);
                stringRedisTemplate.opsForValue().set(cacheKey, jsonStr, 30, TimeUnit.MINUTES);
                System.out.println("🟢 [Service层] 已成功将 MySQL 数据同步至 Redis 缓存！");
            }

            return product;

        } catch (Exception e) {
            // 大厂容灾规范：缓存降级！
            // 万一 Redis 宕机，或者 JSON 解析失败，绝对不能抛出异常让前端报错！
            // 直接吞掉异常，打个日志，然后降级去查数据库兜底！
            System.out.println("🔴 [警告] Redis 异常或 JSON 转换失败，触发缓存降级，直接查询 MySQL 兜底！");
            e.printStackTrace();
            return productMapper.selectById(id);
        }
    }

    // 2. 扣减库存（自带延迟双删与事务）
    @Override
    @Transactional(rollbackFor = Exception.class) // 加上事务，保证数据库操作的原子性
    public String deductStock(Long productId, Integer num) {
        String cacheKey = "product::" + productId;

        // 【双删第一击】
        stringRedisTemplate.delete(cacheKey);
        System.out.println("🧹 [延迟双删] 第一击：已删除旧缓存！");

        // 更新 MySQL
        int rows = productMapper.deductStock(productId, num);

        if (rows > 0) {
            // 【双删第二击】异步延迟删除
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(500);
                    stringRedisTemplate.delete(cacheKey);
                    System.out.println("🧹 [延迟双删] 第二击：延迟 500ms 后再次清理缓存成功！");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            return "SUCCESS";
        }
        return "FAIL";
    }

    @Override
    public List<Product> getRelatedProducts(Long categoryId, Long productId) {
        System.out.println("🟢 [Service层] 正在为分类 " + categoryId + " 查询推荐商品...");
        // 调用你之前在 Mapper 里写好的 getByCategoryIdAndNotId 方法
        return productMapper.getByCategoryIdAndNotId(categoryId, productId);
    }
}