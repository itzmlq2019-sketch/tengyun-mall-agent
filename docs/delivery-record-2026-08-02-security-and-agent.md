# 2026-08-02 安全边界与 AI 接口收口记录

## 改动范围

- 网关拒绝外部访问库存扣减、库存补偿、死信查询和死信重投路径。
- 商品服务使用 `INTERNAL_API_TOKEN` 校验订单服务的库存写操作。
- 订单死信管理接口使用 `ADMIN_API_TOKEN` 校验管理员调用。
- 网关清理客户端传入的 `X-User-Id`、`X-Internal-Token` 和 `X-Admin-Token`，再写入服务端解析的用户身份。
- 用户服务和网关删除固定 JWT 默认密钥，缺少 `JWT_SECRET` 时启动失败。
- AI 对话由可能触发写操作的 GET SSE 接口改为 `POST /agent/chat` JSON 接口。
- 删除无条件 `@CrossOrigin`，并为 DeepSeek 调用增加总耗时超时。
- README 明确当前使用 WebClient 直接调用 DeepSeek OpenAI 兼容 API，不再把普通响应描述为流式输出。

## 自动化测试

执行命令：

```text
mvn clean verify
```

结果：

- 7 个 Maven 模块全部成功。
- 48 个测试通过，0 失败，0 错误，0 跳过。
- 新增覆盖：网关内部路径拒绝、敏感请求头清理、JWT 密钥缺失、内部/管理员令牌校验、死信接口权限、AI POST 请求与旧 GET 接口下线。

## 真实服务验证

在本机 MySQL、Redis、RabbitMQ、Nacos 全部就绪后启动六个服务，执行 `scripts/smoke-test.ps1`，端口、OpenAPI 和输入校验全部通过。

安全访问断言结果：

```text
GATEWAY_INTERNAL=403
DIRECT_INTERNAL_DENIED=403
DIRECT_INTERNAL_ALLOWED=200
ADMIN_DENIED=403
ADMIN_ALLOWED=200
LEGACY_AGENT_GET=404
```

正确内部令牌的测试使用不存在的补偿请求号，仅验证鉴权可达性，没有修改商品库存。

## 尚未完成的生产化验证

- 订单数据库写入失败与购物车服务故障目前只有单元测试，尚未做跨进程故障注入。
- RabbitMQ Publisher Return 已配置，但尚未执行真实不可路由消息验证。
- 多订单消费者实例竞争同一消息尚未做真实集群测试。
- 用户、购物车和 AI 审计数据库尚未全部纳入 Flyway 迁移。
