package com.Baluga.service.impl;

import com.Baluga.entity.Product;
import com.Baluga.mapper.ProductMapper;
import com.Baluga.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ProductServiceImpl implements ProductService {

    private static final String PRODUCT_CACHE_KEY_PREFIX = "product:detail:";
    private static final long PRODUCT_CACHE_TTL_MINUTES = 30L;

    private final ProductMapper productMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public ProductServiceImpl(ProductMapper productMapper,
                              StringRedisTemplate stringRedisTemplate,
                              ObjectMapper objectMapper) {
        this.productMapper = productMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void add(Product product) {
        productMapper.insert(product);
        cacheProduct(product);
    }

    @Override
    public void update(Product product) {
        productMapper.updateById(product);
        if (product.getId() != null) {
            stringRedisTemplate.delete(buildProductCacheKey(product.getId()));
        }
    }

    @Override
    public void delete(Long id) {
        productMapper.deleteById(id);
        stringRedisTemplate.delete(buildProductCacheKey(id));
    }

    @Override
    public List<Product> list() {
        return productMapper.selectList(null);
    }

    @Override
    public Product getById(Long id) {
        String cacheKey = buildProductCacheKey(id);
        String cachedProduct = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cachedProduct)) {
            return readValue(cachedProduct, Product.class);
        }

        Product product = productMapper.selectById(id);
        if (product != null) {
            cacheProduct(product);
        }
        return product;
    }

    private void cacheProduct(Product product) {
        if (product == null || product.getId() == null) {
            return;
        }
        writeValue(buildProductCacheKey(product.getId()), product, PRODUCT_CACHE_TTL_MINUTES);
    }

    private String buildProductCacheKey(Long id) {
        return PRODUCT_CACHE_KEY_PREFIX + id;
    }

    private void writeValue(String key, Object value, long ttlMinutes) {
        try {
            stringRedisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(value),
                    ttlMinutes,
                    TimeUnit.MINUTES
            );
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("product cache serialize error", ex);
        }
    }

    private <T> T readValue(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("product cache deserialize error", ex);
        }
    }
}
