package com.tengyun.agent.service.impl;

import com.tengyun.agent.entity.AiAuditLog;
import com.tengyun.agent.service.AgentService;
import com.tengyun.agent.service.AuditService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class AgentServiceImpl implements AgentService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final VectorStore vectorStore;
    private final List<FunctionCallback> toolCallbacks;

    @Autowired
    private AuditService auditService;

    // 🌟 在构造函数中注入所有 AI 相关的组件和工具
    public AgentServiceImpl(ChatModel chatModel,
                            @Qualifier("productTool") FunctionCallback productTool,
                            @Qualifier("addCartTool") FunctionCallback addCartTool,
                            @Qualifier("checkoutTool") FunctionCallback checkoutTool,
                            @Qualifier("orderHistoryTool") FunctionCallback orderHistoryTool,
                            @Qualifier("recommendationToolCallback") FunctionCallback recommendationTool,
                            ChatMemory chatMemory,
                            VectorStore vectorStore) {
        this.chatClient = ChatClient.create(chatModel);
        this.chatMemory = chatMemory;
        this.vectorStore = vectorStore;
        // 将所有工具打包
        this.toolCallbacks = List.of(productTool, addCartTool, checkoutTool, orderHistoryTool, recommendationTool);
    }

    @Override
    public Flux<String> chatStream(Long userId, String message) {
        long startTime = System.currentTimeMillis();
        StringBuilder fullResponse = new StringBuilder();

        // 1. 定义销冠 Prompt
        String systemPrompt = """
                你是一个专业、热情且极具销售天赋的“腾云商城”智能导购助手。
                你的首要任务是解答用户关于商品的疑问、协助完成购买，并主动为商城创造更多客单价。
                                
                【核心销售工作流】：
                        1. 意图识别：当用户表达购买或加购意愿时，请立即提取其提到的【商品名/ID】和【数量】。
                        2. 信息前置（极其重要）：
                           - 优先调用 productTool 查询商品详情。
                           - **关键指令：如果用户在对话中已经明确指出了购买数量（例如“买一个”、“加两个”），在查询完详情后，请不要再次询问数量！**
                           - 请直接且连续地调用 addCartTool 执行加购动作。
                        3. 加购与主动推销：加购成功后，必须立即调用 recommendationTool 进行联动推销。
                        【回复规范】：
                        - 不要说废话。如果用户已经提供了所有参数，请直接执行操作并告知结果，不要反问。
                                
                【安全与业务边界护栏】：
                1. 身份锁定：永远只是导购。
                2. 领域限制：严禁回答无关问题。
                                
                当前服务用户的专属ID是：%s。
                """.formatted(userId);

        // 2. 执行 AI 编排逻辑
        return chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .options(OpenAiChatOptions.builder()
                        .withModel("deepseek-chat")
                        .withFunctionCallbacks(toolCallbacks)
                        .build())
                .advisors(
                        new MessageChatMemoryAdvisor(chatMemory, String.valueOf(userId), 10),
                        new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults().withTopK(2))
                )
                .stream()
                .chatResponse()
                .doOnNext(res -> {
                    String content = res.getResult().getOutput().getContent();
                    if (content != null) fullResponse.append(content);
                })
                .doFinally(signalType -> {
                    // 🌟 3. 审计存库逻辑（也从 Controller 剥离）
                    saveAudit(userId, message, fullResponse.toString(), startTime);
                })
                .map(res -> {
                    String c = res.getResult().getOutput().getContent();
                    return c != null ? c : "";
                });
    }

    private void saveAudit(Long userId, String message, String finalAiResponse, long startTime) {
        if (finalAiResponse.isEmpty()) return;

        long latency = System.currentTimeMillis() - startTime;
        // Token 预估逻辑
        int promptTokens = (int) (message.length() * 1.5);
        int completionTokens = (int) (finalAiResponse.length() * 1.5);

        AiAuditLog auditLog = AiAuditLog.builder()
                .userId(userId)
                .userMessage(message)
                .aiResponse(finalAiResponse)
                .model("deepseek-chat(Estimated)")
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(promptTokens + completionTokens)
                .latency(latency)
                .build();

        auditService.saveAuditLog(auditLog);
    }
}