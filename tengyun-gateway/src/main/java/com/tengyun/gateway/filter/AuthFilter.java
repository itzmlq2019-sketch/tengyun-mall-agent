package com.tengyun.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;

@Component
public class AuthFilter implements GlobalFilter, Ordered {

    private static final String SECRET_KEY = "TengYunMallSecretKeyMustBeAtLeast256BitsLong==";
    private static final Key KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        // 1. 登录接口白名单放行
        if ("/user/login".equals(path)) {
            return chain.filter(exchange);
        }

        // 2. 获取 Token
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token == null || token.isEmpty()) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        // 3. 验签与身份透传
        try {
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // 核心改变：不仅要验签，还要把里面的 Claims（载荷数据）拿出来
            Claims claims = Jwts.parserBuilder().setSigningKey(KEY).build().parseClaimsJws(token).getBody();

            // 提取我们在 User 服务颁发 Token 时塞进去的 userId
            String userId = String.valueOf(claims.get("userId"));

            // 改写前端的请求，把提取出来的 userId 塞进一个名叫 X-User-Id 的内部请求头里
            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .build();

            // 用包装好的新请求替换老请求
            ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();

            System.out.println("网关保安：验签通过！提取出真实身份 userId=" + userId + "，已塞入内部请求头放行！");

            // 这里传给下游的是带了新 Header 的 mutatedExchange
            return chain.filter(mutatedExchange);

        } catch (Exception e) {
            System.out.println("网关保安：拦截伪造/过期 Token！");
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}