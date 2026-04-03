package com.Baluga.service.impl;

import com.Baluga.Result;
import com.Baluga.dto.SeckillDTO;
import com.Baluga.entity.Order;
import com.Baluga.entity.SeckillOrder;
import com.Baluga.entity.SeckillProduct;
import com.Baluga.mapper.OrderMapper;
import com.Baluga.mapper.SeckillOrderMapper;
import com.Baluga.mapper.SeckillProductMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeckillServiceImplTest {

    private static final String DUPLICATE_MESSAGE = "\u8bf7\u52ff\u91cd\u590d\u4e0b\u5355";
    private static final String SOLD_OUT_MESSAGE = "\u5df2\u552e\u7f44";
    private static final String SUCCESS_MESSAGE = "\u79d2\u6740\u6210\u529f";
    private static final String SYSTEM_BUSY_MESSAGE = "\u7cfb\u7edf\u7e41\u5fd9\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5";
    private static final String FAILED_MESSAGE = "\u79d2\u6740\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5";

    @Mock
    private SeckillProductMapper seckillProductMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private SeckillOrderMapper seckillOrderMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private DefaultRedisScript<Long> seckillStockScript;

    private SeckillServiceImpl seckillService;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                .modules(new JavaTimeModule())
                .build();
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        seckillService = spy(new SeckillServiceImpl(
                seckillProductMapper,
                orderMapper,
                seckillOrderMapper,
                stringRedisTemplate,
                seckillStockScript,
                objectMapper
        ));
    }

    @Test
    void doSeckillShouldReturnDuplicateWhenUserHasOrdered() {
        when(valueOperations.get("seckill:order:100:1")).thenReturn("88");

        Result result = seckillService.doSeckill(buildDto());

        assertEquals(500, result.getCode());
        assertEquals(DUPLICATE_MESSAGE, result.getMessage());
        verify(seckillProductMapper, never()).selectById(anyLong());
        verify(stringRedisTemplate, never()).execute(
                eq(seckillStockScript),
                eq(Collections.singletonList("seckill:stock:1")),
                eq("1")
        );
    }

    @Test
    void doSeckillShouldReturnSoldOutWhenRedisStockDecrementFails() {
        SeckillProduct product = buildSeckillProduct();
        mockPendingOrderAndProductQuery(product);
        when(stringRedisTemplate.execute(seckillStockScript, Collections.singletonList("seckill:stock:1"), "1"))
                .thenReturn(-1L);

        Result result = seckillService.doSeckill(buildDto());

        assertEquals(500, result.getCode());
        assertEquals(SOLD_OUT_MESSAGE, result.getMessage());
        verify(orderMapper, never()).insert(any(Order.class));
        verify(seckillOrderMapper, never()).insert(any(SeckillOrder.class));
        verify(stringRedisTemplate).delete("seckill:order:100:1");
    }

    @Test
    void doSeckillShouldReturnSystemBusyWhenLuaReturnsInvalidStockFlag() {
        SeckillProduct product = buildSeckillProduct();
        mockPendingOrderAndProductQuery(product);
        when(stringRedisTemplate.execute(seckillStockScript, Collections.singletonList("seckill:stock:1"), "1"))
                .thenReturn(-2L);

        Result result = seckillService.doSeckill(buildDto());

        assertEquals(500, result.getCode());
        assertEquals(SYSTEM_BUSY_MESSAGE, result.getMessage());
        verify(orderMapper, never()).insert(any(Order.class));
        verify(seckillOrderMapper, never()).insert(any(SeckillOrder.class));
        verify(stringRedisTemplate).delete("seckill:order:100:1");
    }

    @Test
    void doSeckillShouldReturnSystemBusyWhenLuaReturnsNull() {
        SeckillProduct product = buildSeckillProduct();
        mockPendingOrderAndProductQuery(product);
        when(stringRedisTemplate.execute(seckillStockScript, Collections.singletonList("seckill:stock:1"), "1"))
                .thenReturn(null);

        Result result = seckillService.doSeckill(buildDto());

        assertEquals(500, result.getCode());
        assertEquals(SYSTEM_BUSY_MESSAGE, result.getMessage());
        verify(orderMapper, never()).insert(any(Order.class));
        verify(seckillOrderMapper, never()).insert(any(SeckillOrder.class));
        verify(stringRedisTemplate).delete("seckill:order:100:1");
    }

    @Test
    void doSeckillShouldCreateOrderAfterRedisStockDeduction() {
        SeckillProduct product = buildSeckillProduct();
        mockPendingOrderAndProductQuery(product);
        when(stringRedisTemplate.execute(seckillStockScript, Collections.singletonList("seckill:stock:1"), "1"))
                .thenReturn(9L);
        when(seckillProductMapper.update(any(), any())).thenReturn(1);
        when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(88L);
            return 1;
        });
        when(seckillOrderMapper.insert(any(SeckillOrder.class))).thenReturn(1);

        Result result = seckillService.doSeckill(buildDto());

        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals(SUCCESS_MESSAGE, result.getData());
        verify(valueOperations).set("seckill:order:100:1", "88");
        ArgumentCaptor<SeckillOrder> captor = ArgumentCaptor.forClass(SeckillOrder.class);
        verify(seckillOrderMapper).insert(captor.capture());
        assertEquals(100L, captor.getValue().getUserId());
        assertEquals(1L, captor.getValue().getSeckillProductId());
        assertEquals(88L, captor.getValue().getOrderId());
    }

    @Test
    void doSeckillShouldRollbackRedisWhenDatabaseStepFails() {
        SeckillProduct product = buildSeckillProduct();
        doNothing().when(seckillService).markCurrentTransactionRollbackOnly();
        mockPendingOrderAndProductQuery(product);
        when(stringRedisTemplate.execute(seckillStockScript, Collections.singletonList("seckill:stock:1"), "1"))
                .thenReturn(9L);
        when(seckillProductMapper.update(any(), any())).thenReturn(0);

        Result result = seckillService.doSeckill(buildDto());

        assertEquals(500, result.getCode());
        assertEquals(FAILED_MESSAGE, result.getMessage());
        verify(valueOperations).increment("seckill:stock:1", 1L);
        verify(stringRedisTemplate).delete("seckill:order:100:1");
        verify(seckillService).markCurrentTransactionRollbackOnly();
    }

    private void mockPendingOrderAndProductQuery(SeckillProduct product) {
        when(valueOperations.get(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            if ("seckill:order:100:1".equals(key)) {
                return null;
            }
            if ("seckill:product:1".equals(key)) {
                return null;
            }
            return null;
        });
        when(seckillOrderMapper.selectOne(any())).thenReturn(null);
        when(valueOperations.setIfAbsent("seckill:order:100:1", "PROCESSING", 5L, TimeUnit.MINUTES)).thenReturn(true);
        when(valueOperations.setIfAbsent("seckill:stock:1", "10")).thenReturn(true);
        when(seckillProductMapper.selectById(1L)).thenReturn(product);
    }

    private SeckillDTO buildDto() {
        SeckillDTO dto = new SeckillDTO();
        dto.setUserId(100L);
        dto.setSeckillProductId(1L);
        return dto;
    }

    private SeckillProduct buildSeckillProduct() {
        SeckillProduct product = new SeckillProduct();
        product.setId(1L);
        product.setProductId(101L);
        product.setSeckillPrice(new BigDecimal("1999.00"));
        product.setStock(10);
        product.setStatus(1);
        product.setStartTime(LocalDateTime.now().minusMinutes(10));
        product.setEndTime(LocalDateTime.now().plusMinutes(10));
        return product;
    }
}
