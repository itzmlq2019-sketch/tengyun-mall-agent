# 交付记录：真实基础设施与下单链路验证

交付日期：2026-08-01

## 验证环境

- MySQL：本机 3306，连接成功。
- Redis：本机 6379，连接成功。
- RabbitMQ：Docker 容器，5672 与 15672 正常监听，管理 API 认证成功。
- Nacos：本机 standalone 模式，8848、9848、9849 正常监听，readiness 返回 `OK`。
- 六个微服务：8080 至 8085 全部监听。

## 配置修复

- 将项目 `.env`、`env.example` 和六个服务的 Nacos 默认地址统一为 `127.0.0.1:8848`。
- 新增 `compose.infrastructure.yml`，用于可复现启动 Nacos 与 RabbitMQ。
- 新增 `scripts/infrastructure-up.ps1` 和 `scripts/infrastructure-down.ps1`。

## Nacos 与 RabbitMQ 证据

- Nacos 中 `gateway-service`、`user-service`、`order-service`、`product-service`、`cart-service`、`agent-service` 各有 1 个健康实例。
- RabbitMQ 当前订单队列 `order.queue.v2` 状态正常。
- 真实下单完成后：
  - `order.queue.v2`：消息数 0、待消费 0、未确认 0。
  - `order.dlx.queue.v2`：消息数 0、待消费 0、未确认 0。

## HTTP 冒烟测试

执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
```

结果：

- 6 个服务端口全部通过。
- 6 个 OpenAPI 接口全部返回 200。
- 用户、商品、购物车、订单和 AI 参数校验契约全部通过。

## 真实业务链路

通过网关执行：

1. 测试账号登录成功，JWT 未写入日志或验收记录。
2. 查询商品 1，初始库存 93。
3. 加入购物车，数量 1。
4. 提交异步订单。
5. 轮询订单历史，订单状态变为 `CREATED`。
6. 再次查询商品，库存为 92。
7. 查询购物车，商品 1 已移除。

本次真实测试创建的数据：

```text
requestId: 6f0ebfdb-929d-49ae-95f0-91fcb0991bd9
productId: 1
quantity: 1
orderStatus: CREATED
totalAmount: 14999.00
stockDelta: -1
stockLedgerStatus: DEDUCTED
```

MySQL 只读核对确认：订单表存在且仅有该 `requestId` 对应记录，商品库存流水表存在对应 `DEDUCTED` 记录。

## 仍未通过的项目

- 尚未执行“扣库存成功但 HTTP 响应丢失”的真实网络故障注入。
- 尚未执行订单数据库写入失败后的真实库存补偿测试。
- 尚未执行购物车服务故障后的跨进程重试测试。
- RabbitMQ 重启和消息持久化已在后续故障验证中通过；Publisher Confirm 仍未测试。
- 尚未执行 50 个以上并发订单测试。

因此，本次证明正常真实链路可用，但不关闭订单一致性与 RabbitMQ 可靠性的发布阻断项。
