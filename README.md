# 腾云智能导购商城

腾云智能导购商城是一个基于 Spring Cloud Alibaba 与大语言模型的微服务电商项目。系统将商品查询、加入购物车、订单结算等传统购买流程封装为 AI 可调用的业务工具，使用户能够通过自然语言完成导购和下单。

## 核心功能

- AI 智能导购：识别用户意图并编排商品查询、相关推荐、加购和结算工具。
- 统一网关鉴权：校验 JWT，并向下游服务安全传递用户身份。
- 异步订单处理：通过 RabbitMQ 削峰，将订单提交与后台处理解耦。
- 库存一致性：使用数据库条件更新、请求幂等台账和补偿机制防止超卖与重复扣减。
- 失败消息处理：有限次数重试，超过重试次数后进入死信队列并记录失败原因。
- 商品缓存：使用 Redis 与 Redisson 实现缓存和分布式并发控制。
- 服务注册与发现：各微服务通过 Nacos 注册并由 OpenFeign 调用。

## 技术栈

- Java 17、Spring Boot 3.2.4
- Spring Cloud、Spring Cloud Alibaba
- Nacos、Spring Cloud Gateway、OpenFeign
- MySQL、MyBatis-Plus、Flyway
- Redis、Redisson
- RabbitMQ
- Spring WebClient、DeepSeek OpenAI 兼容 API（当前由项目自行编排工具调用）
- JUnit 5、Mockito

## 服务模块

| 模块 | 默认端口 | 功能 |
| --- | ---: | --- |
| `tengyun-gateway` | 8080 | API 网关、JWT 鉴权、用户上下文传递 |
| `tengyun-user` | 8081 | 用户登录与用户信息 |
| `tengyun-order` | 8082 | 订单提交、异步消费、死信处理 |
| `tengyun-product` | 8083 | 商品查询、库存扣减与补偿 |
| `tengyun-cart` | 8084 | 购物车管理 |
| `tengyun-agent` | 8085 | 大模型接入与导购流程编排 |

## 可靠性设计

### 订单消息可靠投递

- 订单消息使用持久化投递。
- 生产者等待 RabbitMQ Publisher Confirm 后才返回受理成功。
- 开启 Mandatory 和 Publisher Returns，避免消息在无法路由时静默丢失。
- 消费失败采用有限重试，耗尽后转入死信队列。

### 库存幂等与补偿

- 每次结算生成唯一 `requestId`。
- 商品服务通过库存扣减台账识别重复请求。
- 库存使用带数量条件的数据库更新，避免扣成负数。
- 订单写入失败时按同一 `requestId` 补偿库存。
- 消费重试时先检查订单是否已经创建，防止重复扣减。

### 安全与配置

- JWT 密钥、数据库密码和第三方 API 密钥均通过环境变量加载。
- 网关会覆盖客户端伪造的 `X-User-Id` 请求头。
- 登录密码支持 BCrypt，同时兼容已有的明文测试数据。
- API 错误使用统一的 `code/message/data` 响应结构。

## 本地运行

### 1. 准备基础设施

需要启动以下组件：

- MySQL 8.x
- Redis
- RabbitMQ（5672、15672）
- Nacos 单机模式（8848、9848、9849）

项目提供了基础设施编排文件：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\infrastructure-up.ps1
```

如果已经手动启动这些组件，可以跳过此命令。

### 2. 配置环境变量

复制项目根目录的 `env.example` 为 `.env`，然后填写本机配置：

```powershell
Copy-Item env.example .env
```

主要变量包括：

- `MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_USER`、`MYSQL_PASSWORD`
- `USER_DB_NAME`、`ORDER_DB_NAME`、`PRODUCT_DB_NAME`、`CART_DB_NAME`、`AGENT_DB_NAME`
- `NACOS_ADDR`
- `REDIS_HOST`、`REDIS_PORT`、`REDIS_PASSWORD`
- `RABBITMQ_HOST`、`RABBITMQ_PORT`、`RABBITMQ_USER`、`RABBITMQ_PASSWORD`
- `DEEPSEEK_API_KEY`、`DEEPSEEK_BASE_URL`、`DEEPSEEK_MODEL`
- `JWT_SECRET`、`JWT_EXPIRE_HOURS`
- `INTERNAL_API_TOKEN`、`ADMIN_API_TOKEN`
- `AGENT_REQUEST_TIMEOUT_MS`

`.env` 已被 Git 忽略，不要将真实密码或密钥提交到仓库。

### 3. 构建与测试

确保 Java 17 和 Maven 已加入 PATH，然后执行：

```powershell
mvn clean test
```

### 4. 启动服务

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-all.ps1
```

停止全部服务：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-all.ps1
```

## 数据库迁移

订单服务和商品服务使用 Flyway 管理新增表结构：

- `tengyun-order/src/main/resources/db/migration`
- `tengyun-product/src/main/resources/db/migration`

服务启动时会自动执行尚未应用的迁移。已有数据库启用了 `baseline-on-migrate`，用于兼容历史表结构。

## API 文档

服务启动后可访问：

- 网关：`http://127.0.0.1:8080/swagger-ui.html`
- 用户服务：`http://127.0.0.1:8081/swagger-ui.html`
- 订单服务：`http://127.0.0.1:8082/swagger-ui.html`
- 商品服务：`http://127.0.0.1:8083/swagger-ui.html`
- 购物车服务：`http://127.0.0.1:8084/swagger-ui.html`
- AI Agent：`http://127.0.0.1:8085/swagger-ui.html`

AI 对话使用普通 POST 接口，不伪装为流式 SSE：

```http
POST /agent/chat
Authorization: Bearer <token>
Content-Type: application/json

{"message":"推荐一款咖啡"}
```

库存扣减、库存补偿、死信查看和死信重投属于内部管理能力，网关不对普通用户开放。服务间库存调用必须携带 `INTERNAL_API_TOKEN`，死信管理必须携带 `ADMIN_API_TOKEN`。

## 冒烟测试

全部服务启动后执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
```

脚本会检查：

- 六个服务端口是否监听；
- 各服务的 OpenAPI 文档是否可访问；
- 基本参数校验是否正确返回 400，而不是产生 500 异常。

更完整的测试范围和真实基础设施验证结果见 `docs/acceptance-checklist.md` 及相关交付记录。
