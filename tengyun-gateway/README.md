## tengyun-gateway
基于 Spring Cloud Gateway 构建，负责全系统的流量调度与安全防御。

### 核心技术点
- **JWT 统一鉴权**：在网关层进行全局 Token 校验，拦截非法请求。
- **身份透传机制**：解析 Token 后将 `userId` 注入 Header (`X-User-Id`)，确保下游微服务在“零信任”环境下安全获取用户信息。
- **路由分发**：实现各业务模块的动态路由映射。
