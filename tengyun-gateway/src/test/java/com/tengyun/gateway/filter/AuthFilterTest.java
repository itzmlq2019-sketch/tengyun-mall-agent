package com.tengyun.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tengyun.gateway.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthFilterTest {

    @Test
    void shouldReturn401WhenTokenMissing() {
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        AuthFilter authFilter = new AuthFilter(jwtTokenProvider, new ObjectMapper());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/order/history/1").build()
        );

        authFilter.filter(exchange, ex -> Mono.empty()).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void shouldAllowPublicPathWithoutToken() {
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        AuthFilter authFilter = new AuthFilter(jwtTokenProvider, new ObjectMapper());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/user/login").build()
        );
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        authFilter.filter(exchange, ex -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertTrue(chainCalled.get());
    }

    @Test
    void shouldBlockInternalEndpointEvenWithToken() {
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        AuthFilter authFilter = new AuthFilter(jwtTokenProvider, new ObjectMapper());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/product/deduct?requestId=req-1&productId=1&num=1")
                        .header("Authorization", "Bearer valid")
                        .build()
        );
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        authFilter.filter(exchange, ex -> {
            chainCalled.set(true);
            return Mono.empty();
        }).block();

        assertEquals(HttpStatus.FORBIDDEN, exchange.getResponse().getStatusCode());
        assertFalse(chainCalled.get());
    }

    @Test
    void shouldInjectUserIdFromTokenClaims() {
        JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
        Claims claims = Jwts.claims();
        claims.put("userId", 1L);
        when(jwtTokenProvider.parseToken("valid")).thenReturn(claims);

        AuthFilter authFilter = new AuthFilter(jwtTokenProvider, new ObjectMapper());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/order/history/1")
                        .header("Authorization", "Bearer valid")
                        .header("X-User-Id", "999")
                        .header("X-Internal-Token", "forged")
                        .header("X-Admin-Token", "forged")
                        .build()
        );
        AtomicReference<String> userIdHeader = new AtomicReference<>();
        AtomicReference<String> internalHeader = new AtomicReference<>();
        AtomicReference<String> adminHeader = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            userIdHeader.set(ex.getRequest().getHeaders().getFirst("X-User-Id"));
            internalHeader.set(ex.getRequest().getHeaders().getFirst("X-Internal-Token"));
            adminHeader.set(ex.getRequest().getHeaders().getFirst("X-Admin-Token"));
            return Mono.empty();
        };

        authFilter.filter(exchange, chain).block();

        assertEquals("1", userIdHeader.get());
        assertEquals(null, internalHeader.get());
        assertEquals(null, adminHeader.get());
    }
}
