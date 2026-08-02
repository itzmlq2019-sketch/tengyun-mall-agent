package com.tengyun.order.listener;

import com.tengyun.order.client.CartClient;
import com.tengyun.order.client.ProductClient;
import com.tengyun.order.dto.ApiResponse;
import com.tengyun.order.dto.OrderMessageDTO;
import com.tengyun.order.dto.ProductDTO;
import com.tengyun.order.entity.Order;
import com.tengyun.order.mapper.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderMessageListenerTest {

    private static final String INTERNAL_TOKEN = "test-internal-token-at-least-32-characters";

    @Mock
    private RedissonClient redissonClient;
    @Mock
    private ProductClient productClient;
    @Mock
    private CartClient cartClient;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private RLock lock;

    private OrderMessageListener listener;

    @BeforeEach
    void setUp() {
        listener = new OrderMessageListener(redissonClient, productClient, cartClient, orderMapper, INTERNAL_TOKEN);
    }

    @Test
    void shouldOnlyCleanCartWhenOrderAlreadyExists() throws InterruptedException {
        OrderMessageDTO dto = new OrderMessageDTO("req-1", 1L, 2L, 1);
        Order existing = new Order();
        existing.setUserId(1L);
        existing.setProductId(2L);
        when(redissonClient.getLock("lock:product:stock:2")).thenReturn(lock);
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(orderMapper.selectByRequestId("req-1")).thenReturn(existing);

        listener.handleOrderMessage(dto);

        verify(cartClient).removeItem(1L, 2L);
        verify(productClient, never()).getProductInfo(any());
        verify(productClient, never()).deductStock(any(), any(), any(), any());
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    void shouldCreateOrderWhenMessageValid() throws InterruptedException {
        OrderMessageDTO dto = new OrderMessageDTO("req-2", 1L, 2L, 3);
        ProductDTO productDTO = new ProductDTO();
        productDTO.setId(2L);
        productDTO.setName("Latte");
        productDTO.setPrice(new BigDecimal("12.50"));

        when(redissonClient.getLock("lock:product:stock:2")).thenReturn(lock);
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(productClient.getProductInfo(2L)).thenReturn(ApiResponse.success(productDTO));
        when(productClient.deductStock("req-2", 2L, 3, INTERNAL_TOKEN)).thenReturn(ApiResponse.success("SUCCESS"));

        listener.handleOrderMessage(dto);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderMapper, times(1)).insert(orderCaptor.capture());
        Order inserted = orderCaptor.getValue();
        assertEquals("req-2", inserted.getRequestId());
        assertEquals(1L, inserted.getUserId());
        assertEquals(2L, inserted.getProductId());
        assertEquals(3, inserted.getQuantity());
        assertEquals(new BigDecimal("37.50"), inserted.getTotalAmount());
        assertEquals("CREATED", inserted.getStatus());

        verify(cartClient).removeItem(1L, 2L);
        verify(lock).unlock();
    }

    @Test
    void shouldRollbackAndThrowWhenDeductFails() throws InterruptedException {
        OrderMessageDTO dto = new OrderMessageDTO("req-3", 1L, 2L, 1);
        ProductDTO productDTO = new ProductDTO();
        productDTO.setId(2L);
        productDTO.setName("Espresso");
        productDTO.setPrice(new BigDecimal("9.90"));

        when(redissonClient.getLock("lock:product:stock:2")).thenReturn(lock);
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(productClient.getProductInfo(2L)).thenReturn(ApiResponse.success(productDTO));
        when(productClient.deductStock("req-3", 2L, 1, INTERNAL_TOKEN)).thenReturn(ApiResponse.success("FAILED"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> listener.handleOrderMessage(dto));
        assertEquals("ORDER_CONSUME_FAILED", ex.getMessage());
        verify(orderMapper, never()).insert(any(Order.class));
        verify(lock).unlock();
    }

    @Test
    void shouldRejectWhenProductNotFound() throws InterruptedException {
        OrderMessageDTO dto = new OrderMessageDTO("req-4", 1L, 2L, 1);

        when(redissonClient.getLock("lock:product:stock:2")).thenReturn(lock);
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(productClient.getProductInfo(2L)).thenReturn(ApiResponse.success(null));

        AmqpRejectAndDontRequeueException ex =
                assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.handleOrderMessage(dto));
        assertEquals("PRODUCT_NOT_FOUND", ex.getMessage());
        verify(lock).unlock();
    }

    @Test
    void shouldCompensateStockWhenOrderInsertFails() throws InterruptedException {
        OrderMessageDTO dto = new OrderMessageDTO("req-5", 1L, 2L, 1);
        ProductDTO productDTO = new ProductDTO();
        productDTO.setId(2L);
        productDTO.setName("Mocha");
        productDTO.setPrice(new BigDecimal("15.00"));

        when(redissonClient.getLock("lock:product:stock:2")).thenReturn(lock);
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(orderMapper.selectByRequestId("req-5")).thenReturn(null);
        when(productClient.getProductInfo(2L)).thenReturn(ApiResponse.success(productDTO));
        when(productClient.deductStock("req-5", 2L, 1, INTERNAL_TOKEN)).thenReturn(ApiResponse.success("SUCCESS"));
        when(orderMapper.insert(any(Order.class))).thenThrow(new RuntimeException("DB_DOWN"));
        when(productClient.compensateStock("req-5", INTERNAL_TOKEN)).thenReturn(ApiResponse.success("SUCCESS"));

        assertThrows(RuntimeException.class, () -> listener.handleOrderMessage(dto));

        verify(productClient).compensateStock("req-5", INTERNAL_TOKEN);
        verify(cartClient, never()).removeItem(any(), any());
    }

    @Test
    void shouldNotCompensateOrRedeductWhenCartCleanupFails() throws InterruptedException {
        OrderMessageDTO dto = new OrderMessageDTO("req-6", 1L, 2L, 1);
        ProductDTO productDTO = new ProductDTO();
        productDTO.setId(2L);
        productDTO.setName("Americano");
        productDTO.setPrice(new BigDecimal("10.00"));

        when(redissonClient.getLock("lock:product:stock:2")).thenReturn(lock);
        when(lock.tryLock(3, TimeUnit.SECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(orderMapper.selectByRequestId("req-6")).thenReturn(null);
        when(productClient.getProductInfo(2L)).thenReturn(ApiResponse.success(productDTO));
        when(productClient.deductStock("req-6", 2L, 1, INTERNAL_TOKEN)).thenReturn(ApiResponse.success("SUCCESS"));
        doThrow(new RuntimeException("CART_DOWN")).when(cartClient).removeItem(1L, 2L);

        assertThrows(RuntimeException.class, () -> listener.handleOrderMessage(dto));

        verify(orderMapper).insert(any(Order.class));
        verify(productClient, never()).compensateStock(any(), any());
        verify(productClient, times(1)).deductStock("req-6", 2L, 1, INTERNAL_TOKEN);
    }

    @Test
    void shouldRejectInvalidMessage() {
        OrderMessageDTO invalid = new OrderMessageDTO("", 1L, 2L, 0);

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.handleOrderMessage(invalid));
        verify(redissonClient, never()).getLock(any());
    }
}
