package com.Baluga.service.impl;

import com.Baluga.entity.Product;
import com.Baluga.mapper.ProductMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductMapper productMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ProductServiceImpl productService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = Jackson2ObjectMapperBuilder.json().build();
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        productService = new ProductServiceImpl(productMapper, stringRedisTemplate, objectMapper);
    }

    @Test
    void getByIdShouldReturnProductFromRedisFirst() throws Exception {
        Product cachedProduct = buildProduct();
        when(valueOperations.get("product:detail:1")).thenReturn(objectMapper.writeValueAsString(cachedProduct));

        Product result = productService.getById(1L);

        assertNotNull(result);
        assertEquals("iPhone 16", result.getName());
        verifyNoInteractions(productMapper);
    }

    @Test
    void getByIdShouldQueryDbAndBackfillRedisWhenCacheMiss() {
        Product dbProduct = buildProduct();
        when(valueOperations.get("product:detail:1")).thenReturn(null);
        when(productMapper.selectById(1L)).thenReturn(dbProduct);

        Product result = productService.getById(1L);

        assertNotNull(result);
        assertEquals(new BigDecimal("6999.00"), result.getPrice());
        verify(productMapper).selectById(1L);
        verify(valueOperations).set(eq("product:detail:1"), anyString(), eq(30L), eq(TimeUnit.MINUTES));
    }

    private Product buildProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 16");
        product.setDescription("Apple flagship phone");
        product.setPrice(new BigDecimal("6999.00"));
        product.setStock(100);
        product.setStatus(1);
        return product;
    }
}
