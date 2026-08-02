package com.tengyun.order.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tengyun.order.entity.OrderDeadLetterLog;
import com.tengyun.order.mapper.OrderDeadLetterLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderDeadLetterListenerTest {

    @Mock
    private OrderDeadLetterLogMapper deadLetterLogMapper;

    private OrderDeadLetterListener listener;

    @BeforeEach
    void setUp() {
        listener = new OrderDeadLetterListener(new ObjectMapper(), deadLetterLogMapper);
    }

    @Test
    void shouldPersistRequestIdAndReasonFromDeadLetterMessage() {
        MessageProperties properties = new MessageProperties();
        properties.setHeader("x-death", List.of(Map.of("reason", "rejected")));
        Message message = new Message("{\"requestId\":\"req-dlq-1\",\"userId\":1,\"productId\":2,\"quantity\":1}"
                .getBytes(StandardCharsets.UTF_8), properties);

        listener.handleDeadLetter(message);

        ArgumentCaptor<OrderDeadLetterLog> captor = ArgumentCaptor.forClass(OrderDeadLetterLog.class);
        verify(deadLetterLogMapper).insert(captor.capture());
        OrderDeadLetterLog log = captor.getValue();
        assertEquals("req-dlq-1", log.getRequestId());
        assertEquals("rejected", log.getReason());
    }

    @Test
    void shouldFallbackWhenPayloadOrHeadersInvalid() {
        MessageProperties properties = new MessageProperties();
        Message message = new Message("not-json".getBytes(StandardCharsets.UTF_8), properties);

        listener.handleDeadLetter(message);

        ArgumentCaptor<OrderDeadLetterLog> captor = ArgumentCaptor.forClass(OrderDeadLetterLog.class);
        verify(deadLetterLogMapper).insert(captor.capture());
        OrderDeadLetterLog log = captor.getValue();
        assertNull(log.getRequestId());
        assertEquals("UNKNOWN", log.getReason());
    }
}
