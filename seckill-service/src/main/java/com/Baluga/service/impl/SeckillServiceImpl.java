package com.Baluga.service.impl;

import com.Baluga.Result;
import com.Baluga.dto.SeckillDTO;
import com.Baluga.entity.Order;
import com.Baluga.entity.SeckillOrder;
import com.Baluga.entity.SeckillProduct;
import com.Baluga.mapper.OrderMapper;
import com.Baluga.mapper.SeckillOrderMapper;
import com.Baluga.mapper.SeckillProductMapper;
import com.Baluga.service.SeckillService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SeckillServiceImpl implements SeckillService {

    private static final String SECKILL_PRODUCT_KEY_PREFIX = "seckill:product:";
    private static final String SECKILL_STOCK_KEY_PREFIX = "seckill:stock:";
    private static final String SECKILL_ORDER_KEY_PREFIX = "seckill:order:";
    private static final String ORDER_PROCESSING = "PROCESSING";
    private static final long PRODUCT_CACHE_TTL_MINUTES = 30L;
    private static final long ORDER_MARK_TTL_MINUTES = 5L;

    private final SeckillProductMapper seckillProductMapper;
    private final OrderMapper orderMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final DefaultRedisScript<Long> seckillStockScript;
    private final ObjectMapper objectMapper;

    public SeckillServiceImpl(SeckillProductMapper seckillProductMapper,
                              OrderMapper orderMapper,
                              SeckillOrderMapper seckillOrderMapper,
                              StringRedisTemplate stringRedisTemplate,
                              DefaultRedisScript<Long> seckillStockScript,
                              ObjectMapper objectMapper) {
        this.seckillProductMapper = seckillProductMapper;
        this.orderMapper = orderMapper;
        this.seckillOrderMapper = seckillOrderMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.seckillStockScript = seckillStockScript;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void preloadSeckillData() {
        List<SeckillProduct> products = seckillProductMapper.selectList(
                new LambdaQueryWrapper<SeckillProduct>().eq(SeckillProduct::getStatus, 1)
        );
        for (SeckillProduct product : products) {
            cacheSeckillProduct(product);
            initStockCacheIfAbsent(product);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result doSeckill(SeckillDTO dto) {
        if (dto == null || dto.getUserId() == null || dto.getSeckillProductId() == null) {
            return Result.error("参数不能为空");
        }

        Long userId = dto.getUserId();
        Long seckillProductId = dto.getSeckillProductId();
        String orderKey = buildOrderKey(userId, seckillProductId);
        String stockKey = buildStockKey(seckillProductId);

        if (hasOrdered(userId, seckillProductId, orderKey)) {
            return Result.error("请勿重复下单");
        }

        Boolean orderLocked = stringRedisTemplate.opsForValue()
                .setIfAbsent(orderKey, ORDER_PROCESSING, ORDER_MARK_TTL_MINUTES, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(orderLocked)) {
            return Result.error("请勿重复下单");
        }

        boolean stockDeducted = false;
        try {
            SeckillProduct seckillProduct = getSeckillProduct(seckillProductId);
            if (seckillProduct == null) {
                return releaseOrderMark(orderKey, "秒杀商品不存在");
            }

            if (!isSeckillOpen(seckillProduct)) {
                return releaseOrderMark(orderKey, "秒杀未开始或已结束");
            }

            initStockCacheIfAbsent(seckillProduct);
            Long remainStock = stringRedisTemplate.execute(
                    seckillStockScript,
                    Collections.singletonList(stockKey),
                    "1"
            );
            if (remainStock == null) {
                return releaseOrderMark(orderKey, "系统繁忙，请稍后重试");
            }
            if (remainStock < 0) {
                return releaseOrderMark(orderKey, "已售罄");
            }
            stockDeducted = true;

            int updatedRows = seckillProductMapper.update(
                    null,
                    new LambdaUpdateWrapper<SeckillProduct>()
                            .setSql("stock = stock - 1")
                            .eq(SeckillProduct::getId, seckillProductId)
                            .gt(SeckillProduct::getStock, 0)
            );
            if (updatedRows <= 0) {
                throw new IllegalStateException("database stock update failed");
            }

            Order order = new Order();
            order.setUserId(userId);
            order.setTotalAmount(seckillProduct.getSeckillPrice());
            order.setStatus(0);
            orderMapper.insert(order);

            SeckillOrder seckillOrder = new SeckillOrder();
            seckillOrder.setUserId(userId);
            seckillOrder.setSeckillProductId(seckillProductId);
            seckillOrder.setOrderId(order.getId());
            seckillOrderMapper.insert(seckillOrder);

            stringRedisTemplate.opsForValue().set(orderKey, String.valueOf(order.getId()));
            return Result.success("秒杀成功");
        } catch (DuplicateKeyException ex) {
            if (stockDeducted) {
                restoreRedisStock(stockKey);
            }
            markCurrentTransactionRollbackOnly();
            stringRedisTemplate.opsForValue().set(orderKey, "ORDERED");
            return Result.error("请勿重复下单");
        } catch (Exception ex) {
            if (stockDeducted) {
                restoreRedisStock(stockKey);
            }
            markCurrentTransactionRollbackOnly();
            stringRedisTemplate.delete(orderKey);
            return Result.error("秒杀失败，请稍后重试");
        }
    }

    protected void markCurrentTransactionRollbackOnly() {
        TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
    }

    private Result releaseOrderMark(String orderKey, String message) {
        stringRedisTemplate.delete(orderKey);
        return Result.error(message);
    }

    private boolean hasOrdered(Long userId, Long seckillProductId, String orderKey) {
        String orderMark = stringRedisTemplate.opsForValue().get(orderKey);
        if (StringUtils.hasText(orderMark)) {
            return true;
        }

        SeckillOrder existingOrder = seckillOrderMapper.selectOne(
                new LambdaQueryWrapper<SeckillOrder>()
                        .eq(SeckillOrder::getUserId, userId)
                        .eq(SeckillOrder::getSeckillProductId, seckillProductId)
                        .last("limit 1")
        );
        if (existingOrder != null) {
            stringRedisTemplate.opsForValue().set(orderKey, String.valueOf(existingOrder.getOrderId()));
            return true;
        }
        return false;
    }

    private SeckillProduct getSeckillProduct(Long seckillProductId) {
        String cacheKey = buildSeckillProductKey(seckillProductId);
        String cachedProduct = stringRedisTemplate.opsForValue().get(cacheKey);
        if (StringUtils.hasText(cachedProduct)) {
            return readValue(cachedProduct, SeckillProduct.class);
        }

        SeckillProduct seckillProduct = seckillProductMapper.selectById(seckillProductId);
        if (seckillProduct != null) {
            cacheSeckillProduct(seckillProduct);
            initStockCacheIfAbsent(seckillProduct);
        }
        return seckillProduct;
    }

    private void cacheSeckillProduct(SeckillProduct seckillProduct) {
        if (seckillProduct == null || seckillProduct.getId() == null) {
            return;
        }
        writeValue(buildSeckillProductKey(seckillProduct.getId()), seckillProduct, PRODUCT_CACHE_TTL_MINUTES);
    }

    private void initStockCacheIfAbsent(SeckillProduct seckillProduct) {
        Integer stock = seckillProduct.getStock() == null ? 0 : seckillProduct.getStock();
        stringRedisTemplate.opsForValue().setIfAbsent(
                buildStockKey(seckillProduct.getId()),
                String.valueOf(Math.max(stock, 0))
        );
    }

    private void restoreRedisStock(String stockKey) {
        stringRedisTemplate.opsForValue().increment(stockKey, 1L);
    }

    private boolean isSeckillOpen(SeckillProduct seckillProduct) {
        LocalDateTime now = LocalDateTime.now();
        boolean statusValid = seckillProduct.getStatus() != null && seckillProduct.getStatus() == 1;
        boolean startValid = seckillProduct.getStartTime() == null || !now.isBefore(seckillProduct.getStartTime());
        boolean endValid = seckillProduct.getEndTime() == null || !now.isAfter(seckillProduct.getEndTime());
        return statusValid && startValid && endValid;
    }

    private String buildSeckillProductKey(Long seckillProductId) {
        return SECKILL_PRODUCT_KEY_PREFIX + seckillProductId;
    }

    private String buildStockKey(Long seckillProductId) {
        return SECKILL_STOCK_KEY_PREFIX + seckillProductId;
    }

    private String buildOrderKey(Long userId, Long seckillProductId) {
        return SECKILL_ORDER_KEY_PREFIX + userId + ":" + seckillProductId;
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
            throw new IllegalStateException("seckill cache serialize error", ex);
        }
    }

    private <T> T readValue(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("seckill cache deserialize error", ex);
        }
    }
}
