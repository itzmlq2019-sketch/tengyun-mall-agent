package com.tengyun.agent.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tengyun.agent.config.AgentToolConfig;
import com.tengyun.agent.entity.AiAuditLog;
import com.tengyun.agent.service.AgentService;
import com.tengyun.agent.service.AuditService;
import com.tengyun.agent.service.PromptProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AgentServiceImpl implements AgentService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final int maxUserMessageLength;
    private final AgentToolConfig agentToolConfig;
    private final AuditService auditService;
    private final PromptProvider promptProvider;
    private static final Set<String> ALLOWED_TOOLS = new HashSet<>(Arrays.asList(
            "productTool",
            "checkoutTool",
            "addCartTool",
            "orderHistoryTool",
            "recommendationTool"
    ));
    private static final List<Pattern> INPUT_BLOCK_PATTERNS = List.of(
            Pattern.compile("(?i)ignore\\s+all\\s+previous\\s+instructions"),
            Pattern.compile("(?i)reveal\\s+(system|developer)\\s+prompt"),
            Pattern.compile("(?i)print\\s+your\\s+hidden\\s+instructions"),
            Pattern.compile("(?i)(api[_-]?key|secret|token|password)\\s*[:=]")
    );
    private static final List<Pattern> OUTPUT_MASK_PATTERNS = List.of(
            Pattern.compile("sk-[A-Za-z0-9]{16,}"),
            Pattern.compile("(?i)(api[_-]?key|secret|password)\\s*[:=]\\s*\\S+"),
            Pattern.compile("eyJ[A-Za-z0-9_-]{20,}\\.[A-Za-z0-9_-]{20,}\\.[A-Za-z0-9_-]{10,}")
    );

    public AgentServiceImpl(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            AgentToolConfig agentToolConfig,
            AuditService auditService,
            PromptProvider promptProvider,
            @Value("${spring.ai.openai.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${spring.ai.openai.api-key:${DEEPSEEK_API_KEY:}}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model:deepseek-chat}") String model,
            @Value("${agent.safety.max-user-message-length:2000}") int maxUserMessageLength
    ) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.agentToolConfig = agentToolConfig;
        this.auditService = auditService;
        this.promptProvider = promptProvider;
        this.apiKey = apiKey;
        this.model = model;
        this.maxUserMessageLength = maxUserMessageLength;
    }

    @Override
    public Flux<String> chatStream(Long userId, String message) {
        long startTime = System.currentTimeMillis();

        if (apiKey == null || apiKey.isBlank()) {
            return Flux.just("AGENT_ERROR: DEEPSEEK_API_KEY_NOT_CONFIGURED");
        }
        if (message == null || message.isBlank()) {
            return Flux.just("AGENT_ERROR: EMPTY_MESSAGE");
        }
        if (message.length() > maxUserMessageLength) {
            return Flux.just("AGENT_BLOCKED: INPUT_TOO_LONG");
        }
        String inputBlockedReason = checkInputRisk(message);
        if (inputBlockedReason != null) {
            return Flux.just("AGENT_BLOCKED: " + inputBlockedReason);
        }

        return Mono.fromCallable(() -> callDeepSeekWithTools(userId, message))
                .map(this::maskSensitiveOutput)
                .doOnSuccess(content -> saveAudit(userId, message, content, startTime))
                .onErrorResume(ex -> Mono.just("AGENT_ERROR: " + ex.getClass().getSimpleName()))
                .flux();
    }

    private String callDeepSeekWithTools(Long userId, String message) throws Exception {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", promptProvider.getSystemPrompt()));
        messages.add(Map.of("role", "user", "content", message));

        Map<String, Object> firstPayload = new HashMap<>();
        firstPayload.put("model", model);
        firstPayload.put("stream", false);
        firstPayload.put("messages", messages);
        firstPayload.put("tools", buildToolDefinitions());
        firstPayload.put("tool_choice", "auto");

        JsonNode firstRoot = postChatCompletion(firstPayload);
        JsonNode assistantMessage = firstRoot.path("choices").path(0).path("message");
        JsonNode toolCalls = assistantMessage.path("tool_calls");

        if (!toolCalls.isArray() || toolCalls.isEmpty()) {
            return parseAssistantContent(firstRoot);
        }

        Map<String, Object> assistantWithTools = new HashMap<>();
        assistantWithTools.put("role", "assistant");
        if (!assistantMessage.path("content").isMissingNode() && !assistantMessage.path("content").isNull()) {
            assistantWithTools.put("content", assistantMessage.path("content").asText());
        } else {
            assistantWithTools.put("content", "");
        }
        assistantWithTools.put("tool_calls", objectMapper.convertValue(toolCalls, new TypeReference<List<Map<String, Object>>>() {
        }));
        messages.add(assistantWithTools);

        for (JsonNode toolCall : toolCalls) {
            String toolCallId = toolCall.path("id").asText();
            String toolName = toolCall.path("function").path("name").asText();
            String arguments = toolCall.path("function").path("arguments").asText("{}");
            String toolResult = executeTool(toolName, arguments, userId);
            messages.add(Map.of(
                    "role", "tool",
                    "tool_call_id", toolCallId,
                    "content", toolResult
            ));
        }

        Map<String, Object> secondPayload = new HashMap<>();
        secondPayload.put("model", model);
        secondPayload.put("stream", false);
        secondPayload.put("messages", messages);
        secondPayload.put("tools", buildToolDefinitions());

        JsonNode secondRoot = postChatCompletion(secondPayload);
        return parseAssistantContent(secondRoot);
    }

    private JsonNode postChatCompletion(Map<String, Object> payload) throws Exception {
        String response = webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        if (response == null || response.isBlank()) {
            throw new IllegalStateException("EMPTY_RESPONSE");
        }
        return objectMapper.readTree(response);
    }

    private String parseAssistantContent(JsonNode root) {
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.isNull()) {
            return "AGENT_ERROR: INVALID_RESPONSE";
        }
        return contentNode.asText();
    }

    private String executeTool(String toolName, String arguments, Long userId) {
        try {
            if (!ALLOWED_TOOLS.contains(toolName)) {
                return "{\"error\":\"TOOL_NOT_ALLOWED\"}";
            }
            JsonNode args = objectMapper.readTree(arguments == null || arguments.isBlank() ? "{}" : arguments);
            return switch (toolName) {
                case "productTool" -> {
                    Long productId = readLong(args, "productId");
                    if (productId == null || productId <= 0) {
                        yield "{\"error\":\"INVALID_PRODUCT_ID\"}";
                    }
                    yield objectMapper.writeValueAsString(
                            agentToolConfig.productTool(new AgentToolConfig.ProductRequest(productId))
                    );
                }
                case "checkoutTool" -> {
                    Long productId = readLong(args, "productId");
                    Integer quantity = readInt(args, "quantity");
                    if (userId == null || productId == null || productId <= 0 || quantity == null || quantity <= 0 || quantity > 99) {
                        yield "{\"error\":\"INVALID_CHECKOUT_ARGS\"}";
                    }
                    yield objectMapper.writeValueAsString(
                            agentToolConfig.checkoutTool(new AgentToolConfig.CheckoutRequest(userId, productId, quantity))
                    );
                }
                case "addCartTool" -> {
                    Long productId = readLong(args, "productId");
                    Integer num = readInt(args, "num");
                    if (userId == null || productId == null || productId <= 0 || num == null || num <= 0 || num > 99) {
                        yield "{\"error\":\"INVALID_CART_ARGS\"}";
                    }
                    yield objectMapper.writeValueAsString(
                            agentToolConfig.addCartTool(new AgentToolConfig.CartRequest(
                                    userId,
                                    productId,
                                    readText(args, "productName"),
                                    readDecimal(args, "price"),
                                    num
                            ))
                    );
                }
                case "orderHistoryTool" -> objectMapper.writeValueAsString(
                        agentToolConfig.orderHistoryTool(new AgentToolConfig.HistoryRequest(userId))
                );
                case "recommendationTool" -> {
                    Long productId = readLong(args, "productId");
                    if (productId == null || productId <= 0) {
                        yield "{\"error\":\"INVALID_PRODUCT_ID\"}";
                    }
                    yield objectMapper.writeValueAsString(
                            agentToolConfig.recommendationTool(new AgentToolConfig.RecommendRequest(productId))
                    );
                }
                default -> "{\"error\":\"UNKNOWN_TOOL\"}";
            };
        } catch (Exception e) {
            return "{\"error\":\"TOOL_EXECUTION_FAILED\"}";
        }
    }

    private Long readLong(JsonNode args, String key) {
        JsonNode v = args.path(key);
        return v.isMissingNode() || v.isNull() ? null : v.asLong();
    }

    private Integer readInt(JsonNode args, String key) {
        JsonNode v = args.path(key);
        return v.isMissingNode() || v.isNull() ? null : v.asInt();
    }

    private String readText(JsonNode args, String key) {
        JsonNode v = args.path(key);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }

    private BigDecimal readDecimal(JsonNode args, String key) {
        JsonNode v = args.path(key);
        if (v.isMissingNode() || v.isNull()) {
            return null;
        }
        return v.isNumber() ? v.decimalValue() : new BigDecimal(v.asText());
    }

    private List<Map<String, Object>> buildToolDefinitions() {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(buildTool("productTool", "Query a product by id", Map.of(
                "type", "object",
                "properties", Map.of("productId", Map.of("type", "integer")),
                "required", List.of("productId")
        )));
        tools.add(buildTool("checkoutTool", "Checkout a product", Map.of(
                "type", "object",
                "properties", Map.of(
                        "productId", Map.of("type", "integer"),
                        "quantity", Map.of("type", "integer")
                ),
                "required", List.of("productId", "quantity")
        )));
        tools.add(buildTool("addCartTool", "Add product to cart", Map.of(
                "type", "object",
                "properties", Map.of(
                        "productId", Map.of("type", "integer"),
                        "num", Map.of("type", "integer")
                ),
                "required", List.of("productId", "num")
        )));
        tools.add(buildTool("orderHistoryTool", "Get order history of current user", Map.of(
                "type", "object",
                "properties", Map.of()
        )));
        tools.add(buildTool("recommendationTool", "Recommend related products", Map.of(
                "type", "object",
                "properties", Map.of("productId", Map.of("type", "integer")),
                "required", List.of("productId")
        )));
        return tools;
    }

    private Map<String, Object> buildTool(String name, String description, Map<String, Object> parameters) {
        Map<String, Object> function = new HashMap<>();
        function.put("name", name);
        function.put("description", description);
        function.put("parameters", parameters);

        Map<String, Object> tool = new HashMap<>();
        tool.put("type", "function");
        tool.put("function", function);
        return tool;
    }

    private void saveAudit(Long userId, String message, String finalAiResponse, long startTime) {
        if (finalAiResponse == null || finalAiResponse.isEmpty()) {
            return;
        }

        long latency = System.currentTimeMillis() - startTime;
        int promptTokens = (int) (message.length() * 1.5);
        int completionTokens = (int) (finalAiResponse.length() * 1.5);

        AiAuditLog auditLog = AiAuditLog.builder()
                .userId(userId)
                .userMessage(message)
                .aiResponse(finalAiResponse)
                .model(model + "(Estimated)")
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(promptTokens + completionTokens)
                .latency(latency)
                .build();

        auditService.saveAuditLog(auditLog);
    }

    private String checkInputRisk(String message) {
        for (Pattern pattern : INPUT_BLOCK_PATTERNS) {
            if (pattern.matcher(message).find()) {
                return "UNSAFE_INPUT";
            }
        }
        return null;
    }

    private String maskSensitiveOutput(String output) {
        if (output == null || output.isBlank()) {
            return output;
        }
        String masked = output;
        for (Pattern pattern : OUTPUT_MASK_PATTERNS) {
            masked = pattern.matcher(masked).replaceAll("[REDACTED]");
        }
        return masked;
    }
}
