# Tengyun AI Shopping Assistant (Microservices)

This project is an e-commerce microservice system based on Spring Cloud Alibaba.
Its core value is turning the classic flow (`search -> add to cart -> checkout`) into an AI-driven shopping conversation.

## Tech Stack

- Spring Boot 3.2.4
- Spring Cloud + Spring Cloud Alibaba (Nacos, Gateway, OpenFeign)
- MySQL + MyBatis-Plus
- Redis + Redisson
- RabbitMQ
- DeepSeek API (through `tengyun-agent`)

## Modules

- `tengyun-gateway`: API gateway, JWT auth, user context forwarding
- `tengyun-user`: login and user info
- `tengyun-product`: product and stock
- `tengyun-cart`: cart management
- `tengyun-order`: checkout and order history
- `tengyun-agent`: AI shopping assistant

## Resume-Ready Improvements

- Security:
  - JWT secret moved from hard-coded value to env var (`JWT_SECRET`)
  - gateway returns structured `401` JSON errors
  - gateway overwrites incoming `X-User-Id` to prevent header spoofing
- Login hardening:
  - query by username, then verify password
  - supports BCrypt hash while keeping plain-text compatibility for old data
  - unified login response (`code/message/data`)
- Config hardening:
  - sensitive config moved to `.env`
  - `env.example` provided as template
- API docs:
  - `springdoc-openapi` integrated in all services
- Quality:
  - unit tests for `PasswordService` and `AuthFilter`
- Dev experience:
  - one-click scripts to start/stop all services

## Run Locally

1. Start dependencies: MySQL, Redis, RabbitMQ, Nacos.
2. Create `.env` at project root (copy from `env.example`).
3. Build:

```bash
E:\maven\apache-maven-3.9.15\bin\mvn.cmd clean package -DskipTests
```

4. Start all services:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-all.ps1
```

5. Stop all services:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-all.ps1
```

## Database Migration (Flyway)

`tengyun-order` now uses Flyway migrations under:

`tengyun-order/src/main/resources/db/migration`

- `V1__create_t_order.sql`
- `V2__enhance_t_order_fields.sql`
- `V3__create_dead_letter_log.sql`

On service startup, Flyway auto-applies pending versions.
For existing databases, `baseline-on-migrate=true` is enabled to avoid breaking startup.

## API Docs

After startup, open each service directly:

- gateway: `http://127.0.0.1:8080/swagger-ui.html`
- user: `http://127.0.0.1:8081/swagger-ui.html`
- order: `http://127.0.0.1:8082/swagger-ui.html`
- product: `http://127.0.0.1:8083/swagger-ui.html`
- cart: `http://127.0.0.1:8084/swagger-ui.html`
- agent: `http://127.0.0.1:8085/swagger-ui.html`

## Smoke Test

After all services are up, run:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-test.ps1
```

This script checks:
- all service ports are listening
- all `v3/api-docs` endpoints are reachable
- basic input-validation contracts return `400` instead of `500`

## Key Environment Variables

- `MYSQL_HOST` `MYSQL_PORT` `MYSQL_USER` `MYSQL_PASSWORD`
- `USER_DB_NAME` `PRODUCT_DB_NAME` `ORDER_DB_NAME` `CART_DB_NAME` `AGENT_DB_NAME`
- `NACOS_ADDR`
- `REDIS_HOST` `REDIS_PORT` `REDIS_PASSWORD`
- `RABBITMQ_HOST` `RABBITMQ_PORT` `RABBITMQ_USER` `RABBITMQ_PASSWORD`
- `DEEPSEEK_API_KEY` `DEEPSEEK_BASE_URL` `DEEPSEEK_MODEL`
- `AGENT_SYSTEM_PROMPT_FILE` `AGENT_PROMPT_RELOAD_INTERVAL_MS`
- `JWT_SECRET` `JWT_EXPIRE_HOURS`
