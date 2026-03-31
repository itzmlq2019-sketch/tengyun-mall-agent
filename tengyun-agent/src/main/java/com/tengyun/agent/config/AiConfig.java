package com.tengyun.agent.config;

import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Qualifier;
@Configuration
public class AiConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    /**
     * 1. 注入 DeepSeek
     */
    @Bean
    public OpenAiChatModel chatModel() {
        return new OpenAiChatModel(new OpenAiApi("https://api.deepseek.com", apiKey));
    }

    /**
     * 2. 手动注册：商品查询工具
     */
    @Bean
    public FunctionCallback productTool(AgentToolConfig toolConfig) {
        return FunctionCallbackWrapper.builder(toolConfig::productTool)
                .withName("productTool") // AI 呼叫时的函数名
                .withDescription("根据商品ID查询商品详情，返回名称、价格和库存状态。")
                .withInputType(AgentToolConfig.ProductRequest.class)
                .build();
    }

    /**
     * 3. 手动注册：结算工具
     */
    @Bean
    public FunctionCallback checkoutTool(AgentToolConfig toolConfig) {
        return FunctionCallbackWrapper.builder(toolConfig::checkoutTool)
                .withName("checkoutTool")
                .withDescription("根据用户ID清空购物车并生成真实订单。")
                .withInputType(AgentToolConfig.CheckoutRequest.class)
                .build();
    }
    /**
     * 全局记忆存储器
     * 这里使用基于内存的存储，适合本地开发测试。
     * 如果以后上生产环境，可以换成 RedisChatMemory 存在 Redis 里。
     */
    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }
    @Bean
    public FunctionCallback addCartTool(AgentToolConfig toolConfig) {
        return FunctionCallbackWrapper.builder(toolConfig::addCartTool)
                .withName("addCartTool")
                .withDescription("将指定商品加入购物车。")
                .withInputType(AgentToolConfig.CartRequest.class)
                .build();
    }
    @Value("classpath:faq.txt")
    private Resource faqResource;

    // 2. 构建内存向量数据库
    @Bean
    public VectorStore vectorStore(@Qualifier("customEmbeddingModel") EmbeddingModel embeddingModel) {
        // 创建一个基于内存的向量库
        SimpleVectorStore vectorStore = new SimpleVectorStore(embeddingModel);

        // 读取 txt 文件
        TextReader textReader = new TextReader(faqResource);
        textReader.getCustomMetadata().put("filename", "faq.txt");

        // 将长文本切分成小块 (Chunk)
        TokenTextSplitter textSplitter = new TokenTextSplitter();

        // 存入向量数据库！
        vectorStore.add(textSplitter.apply(textReader.get()));

        System.out.println("✅ 商城知识库加载完成！");
        return vectorStore;
    }
    //官方模型有 Bug，手写一个轻量级的字频向量引擎
    @Bean
    public org.springframework.ai.embedding.EmbeddingModel customEmbeddingModel() {
        return new org.springframework.ai.embedding.EmbeddingModel() {
            @Override
            public org.springframework.ai.embedding.EmbeddingResponse call(org.springframework.ai.embedding.EmbeddingRequest request) {
                java.util.List<org.springframework.ai.embedding.Embedding> list = new java.util.ArrayList<>();
                for (int i = 0; i < request.getInstructions().size(); i++) {
                    list.add(new org.springframework.ai.embedding.Embedding(embedText(request.getInstructions().get(i)), i));
                }
                return new org.springframework.ai.embedding.EmbeddingResponse(list);
            }

            @Override
            public java.util.List<Double> embed(org.springframework.ai.document.Document document) {
                // 拦截底层的调用，使用自己的算法
                return embedText(document.getContent());
            }

            // 核心算法：把文字变成长度为 512 的数字数组（统计字频）
            private java.util.List<Double> embedText(String text) {
                java.util.List<Double> vector = new java.util.ArrayList<>(java.util.Collections.nCopies(512, 0.0));
                if (text != null) {
                    for (char c : text.toCharArray()) {
                        int index = c % 512;
                        vector.set(index, vector.get(index) + 1.0);
                    }
                }
                return vector;
            }
        };
    }
    @Bean
    public FunctionCallback orderHistoryTool(AgentToolConfig toolConfig) {
        return FunctionCallbackWrapper.builder(toolConfig::orderHistoryTool)
                .withName("orderHistoryTool")
                .withDescription("查询当前用户的历史订单列表。")
                .withInputType(AgentToolConfig.HistoryRequest.class)
                .build();
    }
    //在这里把业务逻辑注册给 AI
    @Bean
    public FunctionCallback recommendationToolCallback(AgentToolConfig toolConfig) {
        return FunctionCallbackWrapper.builder(toolConfig::recommendationTool)
                .withName("recommendationTool") // AI 看到的工具名字
                .withDescription("根据商品ID查询关联推荐商品。当用户加购成功后，必须调用此工具查看是否有配套商品可以推销。") // 给 AI 看的说明书
                .withInputType(AgentToolConfig.RecommendRequest.class)
                .build();
    }
}