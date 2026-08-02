package com.tengyun.order.controller;

import com.tengyun.order.entity.Order;
import com.tengyun.order.exception.GlobalExceptionHandler;
import com.tengyun.order.service.DeadLetterService;
import com.tengyun.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;
    @Mock
    private DeadLetterService deadLetterService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        OrderController controller = new OrderController(orderService, deadLetterService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldUseHeaderUserIdForCheckoutInsteadOfBodyUserId() throws Exception {
        when(orderService.checkout(1L, 2L, 3)).thenReturn("ORDER_ACCEPTED:req-1");

        mockMvc.perform(post("/order/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "1")
                        .content("""
                                {"userId":999,"productId":2,"quantity":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data").value("ORDER_ACCEPTED:req-1"));

        verify(orderService).checkout(1L, 2L, 3);
    }

    @Test
    void shouldReturn400WhenCheckoutHeaderMissing() throws Exception {
        mockMvc.perform(post("/order/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"productId":2,"quantity":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldUseHeaderUserIdForHistory() throws Exception {
        Order order = new Order();
        order.setId(100L);
        when(orderService.getHistory(7L)).thenReturn(List.of(order));

        mockMvc.perform(get("/order/history").header("X-User-Id", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data[0].id").value(100));

        verify(orderService).getHistory(7L);
    }
}
