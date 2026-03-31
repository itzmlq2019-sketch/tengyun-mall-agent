# 腾云智能导购商城 (Tengyun AI E-Commerce)

本项目是一个基于 Spring Cloud 微服务架构与大语言模型 (LLM) 深度结合的新零售电商平台。核心创新点在于通过 Spring AI 框架的 Function Calling 能力，将传统的电商购买链路（商品查询、加购、订单结算）重构为基于自然语言的智能导购对话流（LUI）。

## 核心架构与技术亮点

### 1. AI Agent 导购链路编排
- **微服务工具化封装**：将底层微服务接口（查询、加购、关联推荐、结账）通过 `@Tool` 注解暴露给大语言模型。AI 可根据用户意图，在单次对话中自主决策并进行多工具的链式调用。
- **全链路参数安全透传**：通过定制 DTO 与 Feign 客户端，解决大模型提取参数在 Agent -> Gateway -> 业务微服务间的类型丢失与跨域传输问题。

### 2. 高并发下单与防御体系
- **RabbitMQ 异步削峰**：针对大模型快速决策带来的瞬时订单洪峰，将同步下单与扣减库存逻辑解耦。Agent 投递消息后直接响应，后台消费者监听队列异步处理，提升吞吐量。
- **双重防超卖机制**：底层依赖 MySQL 乐观锁进行数据兜底，应用层利用 Redisson 实现基于商品 ID 的细粒度分布式锁，在并发场景下确保库存扣减的绝对安全。

### 3. 缓存优化与网关鉴权
- **Cache Aside 模式重构**：弃用简单的 Spring Cache 注解，手动实现旁路缓存逻辑。引入 Jackson 解决 Redis 对象序列化异常，并采用延迟双删策略，保障高负载下的缓存与数据库最终一致性。
- **Spring Cloud Gateway 全局鉴权**：构建统一 API 网关，拦截并校验 JWT Token。鉴权通过后，将 `X-User-Id` 写入请求头透传至下游微服务，实现内部调用的绝对信任与安全隔离。

## 技术栈选型

- **基础框架**：Spring Boot 3.x, Spring Cloud Alibaba (Nacos, OpenFeign, Gateway)
- **AI 框架**：Spring AI
- **存储与缓存**：MySQL 8.0, MyBatis-Plus, Redis, Redisson
- **消息队列**：RabbitMQ

## 项目结构

```text
tengyun-parent
├── tengyun-gateway    # API 网关 (全局鉴权、路由分发)
├── tengyun-user       # 用户微服务 (Token 颁发)
├── tengyun-product    # 商品微服务 (库存扣减、Redis 缓存维护)
├── tengyun-order      # 订单微服务 (订单落库、RabbitMQ 消费、分布式锁)
└── tengyun-agent      # 智能中枢 (LLM 接入、业务编排)
