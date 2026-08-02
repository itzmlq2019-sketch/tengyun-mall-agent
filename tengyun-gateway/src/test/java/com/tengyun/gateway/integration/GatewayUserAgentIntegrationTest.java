package com.tengyun.gateway.integration;

import com.tengyun.gateway.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.nacos.discovery.enabled=false",
                "spring.cloud.gateway.routes[0].id=user-test-route",
                "spring.cloud.gateway.routes[0].uri=forward:/__mock/user",
                "spring.cloud.gateway.routes[0].predicates[0]=Path=/user/**",
                "spring.cloud.gateway.routes[1].id=agent-test-route",
                "spring.cloud.gateway.routes[1].uri=forward:/__mock/agent",
                "spring.cloud.gateway.routes[1].predicates[0]=Path=/agent/**"
        }
)
@AutoConfigureWebTestClient
@Import(GatewayUserAgentIntegrationTest.MockDownstreamController.class)
class GatewayUserAgentIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        Claims claims = Jwts.claims();
        claims.put("userId", 88L);
        when(jwtTokenProvider.parseToken("valid-token")).thenReturn(claims);
    }

    @Test
    void shouldForwardUserRequestWithInjectedUserId() {
        webTestClient.get()
                .uri("/user/me")
                .header("Authorization", "Bearer valid-token")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.module").isEqualTo("user")
                .jsonPath("$.userId").isEqualTo("88");
    }

    @Test
    void shouldForwardAgentRequestWithInjectedUserId() {
        webTestClient.get()
                .uri("/agent/whoami")
                .header("Authorization", "Bearer valid-token")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.module").isEqualTo("agent")
                .jsonPath("$.userId").isEqualTo("88");
    }

    @Test
    void shouldReturn401WhenTokenMissing() {
        webTestClient.get()
                .uri("/agent/whoami")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.code").isEqualTo(401)
                .jsonPath("$.message").isEqualTo("TOKEN_MISSING");
    }

    @RestController
    static class MockDownstreamController {

        @GetMapping("/__mock/user")
        public Map<String, Object> mockUser(@RequestHeader(value = "X-User-Id", required = false) String userId) {
            return Map.of("module", "user", "userId", userId);
        }

        @GetMapping("/__mock/agent")
        public Map<String, Object> mockAgent(@RequestHeader(value = "X-User-Id", required = false) String userId) {
            return Map.of("module", "agent", "userId", userId);
        }
    }
}
