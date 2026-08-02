package com.tengyun.agent.controller;

import com.tengyun.agent.exception.GlobalExceptionHandler;
import com.tengyun.agent.service.AgentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AgentControllerTest {

    @Mock
    private AgentService agentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AgentController(agentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldUsePostBodyAndAuthenticatedUserHeader() throws Exception {
        when(agentService.chat(7L, "推荐咖啡")).thenReturn("推荐拿铁");

        mockMvc.perform(post("/agent/chat")
                        .header("X-User-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"推荐咖啡\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("推荐拿铁"));

        verify(agentService).chat(7L, "推荐咖啡");
    }

    @Test
    void shouldRejectMissingMessage() throws Exception {
        mockMvc.perform(post("/agent/chat")
                        .header("X-User-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("MESSAGE_REQUIRED"));
    }

    @Test
    void shouldNotExposeLegacyGetEndpoint() throws Exception {
        mockMvc.perform(get("/agent/chat/stream").param("message", "hello"))
                .andExpect(status().isNotFound());
    }
}
