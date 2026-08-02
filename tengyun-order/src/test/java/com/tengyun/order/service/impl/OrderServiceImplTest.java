package com.tengyun.order.service.impl;

import com.tengyun.order.config.RabbitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl();
        ReflectionTestUtils.setField(service, "rabbitTemplate", rabbitTemplate);
    }

    @Test
    void shouldReturnAcceptedOnlyAfterBrokerAck() {
        completePublishWith(false, null);

        String result = service.checkout(1L, 2L, 1);

        assertTrue(result.startsWith("ORDER_ACCEPTED:"));
    }

    @Test
    void shouldFailCheckoutWhenBrokerNacksMessage() {
        completePublishWith(true, "BROKER_REJECTED");

        assertThrows(IllegalStateException.class, () -> service.checkout(1L, 2L, 1));
    }

    private void completePublishWith(boolean nack, String reason) {
        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(4);
            correlationData.getFuture().complete(new CorrelationData.Confirm(!nack, reason));
            return null;
        }).when(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.ORDER_EXCHANGE),
                eq(RabbitConfig.ORDER_ROUTING_KEY),
                any(),
                any(MessagePostProcessor.class),
                any(CorrelationData.class)
        );
    }
}
