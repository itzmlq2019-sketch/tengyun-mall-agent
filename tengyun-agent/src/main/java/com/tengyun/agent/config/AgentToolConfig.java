package com.tengyun.agent.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.tengyun.agent.client.CartClient;
import com.tengyun.agent.client.OrderClient;
import com.tengyun.agent.client.ProductClient;
import com.tengyun.agent.dto.ApiResponse;
import com.tengyun.agent.dto.CheckoutDTO;
import com.tengyun.agent.dto.ProductDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Component("agentTools")
public class AgentToolConfig {

    private final ProductClient productClient;
    private final OrderClient orderClient;
    private final CartClient cartClient;

    public AgentToolConfig(ProductClient productClient, OrderClient orderClient, CartClient cartClient) {
        this.productClient = productClient;
        this.orderClient = orderClient;
        this.cartClient = cartClient;
    }

    public ProductResponse productTool(ProductRequest request) {
        ApiResponse<ProductDTO> resp = productClient.getProductInfo(request.productId());
        ProductDTO dto = resp == null ? null : resp.data();
        if (dto == null) {
            return new ProductResponse("PRODUCT_NOT_FOUND", null, 0);
        }
        return new ProductResponse(dto.getName(), dto.getPrice(), dto.getStock());
    }

    public String checkoutTool(CheckoutRequest request) {
        if (request == null || request.userId() == null || request.productId() == null
                || request.quantity() == null || request.quantity() <= 0) {
            return "CHECKOUT_FAILED: INVALID_REQUEST";
        }
        CheckoutDTO dto = new CheckoutDTO();
        dto.setProductId(request.productId());
        dto.setQuantity(request.quantity());
        ApiResponse<String> response = orderClient.checkout(request.userId(), dto);
        return response == null ? "CHECKOUT_FAILED: EMPTY_RESPONSE" : response.data();
    }

    public CartResponse addCartTool(CartRequest request) {
        if (request == null) {
            return new CartResponse("ADD_CART_FAILED: EMPTY_REQUEST");
        }
        if (request.userId() == null) {
            return new CartResponse("ADD_CART_FAILED: MISSING_USER_ID");
        }
        if (request.productId() == null) {
            return new CartResponse("ADD_CART_FAILED: MISSING_PRODUCT_ID");
        }
        if (request.num() == null || request.num() <= 0) {
            return new CartResponse("ADD_CART_FAILED: INVALID_QUANTITY");
        }

        ApiResponse<ProductDTO> productResp = productClient.getProductInfo(request.productId());
        ProductDTO product = productResp == null ? null : productResp.data();
        if (product == null) {
            return new CartResponse("ADD_CART_FAILED: PRODUCT_NOT_FOUND");
        }
        if (product.getPrice() == null) {
            return new CartResponse("ADD_CART_FAILED: INVALID_PRODUCT_PRICE");
        }
        if (product.getStock() == null || product.getStock() < request.num()) {
            return new CartResponse("ADD_CART_FAILED: OUT_OF_STOCK");
        }

        ApiResponse<String> addCartResp = cartClient.addCart(
                request.userId(),
                product.getId(),
                product.getName(),
                product.getPrice().toPlainString(),
                request.num()
        );
        return new CartResponse(addCartResp == null ? "ADD_CART_FAILED: EMPTY_RESPONSE" : addCartResp.data());
    }

    public List<OrderHistoryDTO> orderHistoryTool(HistoryRequest request) {
        if (request == null || request.userId() == null) {
            return Collections.emptyList();
        }
        try {
            ApiResponse<List<OrderHistoryDTO>> resp = orderClient.getHistory(request.userId());
            List<OrderHistoryDTO> history = resp == null ? null : resp.data();
            return history == null ? Collections.emptyList() : history;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public RecommendResponse recommendationTool(RecommendRequest request) {
        if (request == null || request.productId() == null) {
            return new RecommendResponse(List.of());
        }
        try {
            ApiResponse<ProductDTO> currentResp = productClient.getProductInfo(request.productId());
            ProductDTO currentProduct = currentResp == null ? null : currentResp.data();
            if (currentProduct == null || currentProduct.getCategoryId() == null) {
                return new RecommendResponse(List.of());
            }

            ApiResponse<List<ProductDTO>> relatedResp =
                    productClient.getRelatedProducts(currentProduct.getCategoryId(), request.productId());
            List<ProductDTO> relatedProducts = relatedResp == null ? List.of() : relatedResp.data();
            if (relatedProducts == null) {
                relatedProducts = List.of();
            }

            List<RecommendItem> items = relatedProducts.stream()
                    .map(p -> new RecommendItem(p.getId(), p.getName(), p.getPrice(), "RELATED"))
                    .toList();
            return new RecommendResponse(items);
        } catch (Exception e) {
            return new RecommendResponse(List.of());
        }
    }

    public record CartRequest(
            @JsonPropertyDescription("Current user id") Long userId,
            @JsonPropertyDescription("Product id") Long productId,
            @JsonPropertyDescription("Product name") String productName,
            @JsonPropertyDescription("Unit price") BigDecimal price,
            @JsonPropertyDescription("Quantity") Integer num
    ) {}

    public record CartResponse(String result) {}

    public record HistoryRequest(Long userId) {}

    public record OrderHistoryDTO(
            Long id,
            String requestId,
            Long productId,
            String productName,
            BigDecimal price,
            Integer quantity,
            String status,
            BigDecimal totalAmount,
            LocalDateTime createTime
    ) {}

    public record RecommendRequest(Long productId) {}

    public record RecommendItem(Long id, String name, BigDecimal price, String tag) {}

    public record RecommendResponse(List<RecommendItem> recommendations) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ProductRequest(@JsonPropertyDescription("Product id to query") Long productId) {}

    public record ProductResponse(String name, BigDecimal price, int stock) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CheckoutRequest(
            @JsonPropertyDescription("Current user id") Long userId,
            @JsonPropertyDescription("Product id to checkout") Long productId,
            @JsonPropertyDescription("Quantity") Integer quantity
    ) {}

    public record CheckoutResponse(String result) {}
}
