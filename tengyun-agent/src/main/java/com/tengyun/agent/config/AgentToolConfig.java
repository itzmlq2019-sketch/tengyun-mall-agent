package com.tengyun.agent.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.tengyun.agent.client.OrderClient;
import com.tengyun.agent.client.ProductClient;
import com.tengyun.agent.dto.CheckoutDTO;
import com.tengyun.agent.dto.ProductDTO;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Component("agentTools")
public class AgentToolConfig {

    private final ProductClient productClient;
    private final OrderClient orderClient;

    public AgentToolConfig(ProductClient productClient, OrderClient orderClient) {
        this.productClient = productClient;
        this.orderClient = orderClient;
    }

    @Tool(description = "根据商品ID查询商品详情，返回商品名称、价格和当前库存状态。在下单或加入购物车前必须调用此工具确认。")
    public ProductResponse productTool(ProductRequest request) {
        System.out.println(" AI Agent 正在调用工具：查询商品详情，ID: " + request.productId());
        ProductDTO dto = productClient.getProductInfo(request.productId());

        if (dto == null) {
            return new ProductResponse("商品不存在", null, 0);
        }
        return new ProductResponse(dto.getName(), dto.getPrice(), dto.getStock());
    }

    @Tool(description = "当用户确定购买、结账或结算某个商品时调用。")
    public String checkoutTool(CheckoutRequest request) {
        System.out.println(" AI 触发结账 -> 商品: " + request.productId());

        CheckoutDTO dto = new CheckoutDTO();
        dto.setUserId(request.userId());
        dto.setProductId(request.productId());
        dto.setQuantity(request.quantity());

        return orderClient.checkout(dto);
    }

    @Tool(description = "将指定商品加入购物车。当用户表达出‘我想买’、‘加购’等意图时，调用此工具。")
    public CartResponse addCartTool(CartRequest request) {
        System.out.println(" AI Agent 正在调用工具：加入购物车，商品ID: " + request.productId() + ", 数量: " + request.num());
        return new CartResponse("已成功将 " + request.num() + " 件商品加入购物车，请问是否现在为您一键结算？");
    }

    public record CartRequest(
            @JsonPropertyDescription("当前用户的ID") Long userId,
            @JsonPropertyDescription("商品ID") Long productId,
            @JsonPropertyDescription("商品名称") String productName,
            @JsonPropertyDescription("单价") BigDecimal price,
            @JsonPropertyDescription("购买数量") Integer num
    ) {}
    public record CartResponse(String result) {}

    // 激活历史订单查询
    @Tool(description = "查询当前用户的历史订单列表。当用户询问'我买过什么'、'我的订单'时调用。")
    public List<OrderHistoryDTO> orderHistoryTool(HistoryRequest request) {
        System.out.println(" AI Agent 正在通过 RPC 调取用户 " + request.userId() + " 的真实历史订单...");
        try {
            List<OrderHistoryDTO> history = orderClient.getHistory(request.userId());
            if (history == null || history.isEmpty()) {
                return Collections.emptyList();
            }
            return history;
        } catch (Exception e) {
            System.err.println("❌ 获取订单历史失败: " + e.getMessage());
            return Collections.emptyList(); // 降级处理，防止 AI 崩溃
        }
    }

    public record HistoryRequest(Long userId) {}

    public record OrderHistoryDTO(Long id, Long productId, String productName, BigDecimal price) {}

    @Tool(description = "根据商品ID查询关联推荐商品。当用户加购商品成功后，必须第一时间调用此工具，查看是否有配套商品可以推销。")
    public RecommendResponse recommendationTool(RecommendRequest request) {
        Long productId = request.productId();
        System.out.println(" AI 正在查询商品 " + request.productId() + " 的关联推荐...");

        try {
            ProductDTO currentProduct = productClient.getProductInfo(productId);
            if (currentProduct == null || currentProduct.getCategoryId() == null) {
                return new RecommendResponse(List.of());
            }

            List<ProductDTO> relatedProducts = productClient.getRelatedProducts(currentProduct.getCategoryId(), productId);

            List<RecommendItem> items = relatedProducts.stream()
                    .map(p -> new RecommendItem(
                            p.getId(),
                            p.getName(),
                            p.getPrice(),
                            "同类热卖组合"
                    ))
                    .toList();

            return new RecommendResponse(items);

        } catch (Exception e) {
            System.err.println("❌ 推荐工具 RPC 调用失败: " + e.getMessage());
            return new RecommendResponse(List.of());
        }
    }

    public record RecommendRequest(Long productId) {}
    public record RecommendItem(Long id, String name, BigDecimal price, String tag) {}
    public record RecommendResponse(List<RecommendItem> recommendations) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ProductRequest(@JsonPropertyDescription("要查询的商品ID") Long productId) {}
    public record ProductResponse(String name, BigDecimal price, int stock) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CheckoutRequest(
            @JsonPropertyDescription("当前用户的ID") Long userId,
            @JsonPropertyDescription("要结算的商品ID") Long productId,
            @JsonPropertyDescription("购买的数量") Integer quantity
    ) {}
    public record CheckoutResponse(String result) {}
}