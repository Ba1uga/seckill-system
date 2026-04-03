package com.Baluga.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisConfigTest {

    @Test
    void seckillStockScriptShouldLoadLuaResourceWithStockValidation() {
        RedisConfig redisConfig = new RedisConfig();

        DefaultRedisScript<Long> redisScript = redisConfig.seckillStockScript();

        assertNotNull(redisScript);
        assertTrue(redisScript.getScriptAsString().contains("redis.call('GET', KEYS[1])"));
        assertTrue(redisScript.getScriptAsString().contains("return -2"));
        assertTrue(redisScript.getScriptAsString().contains("redis.call('DECR', KEYS[1])"));
    }
}
