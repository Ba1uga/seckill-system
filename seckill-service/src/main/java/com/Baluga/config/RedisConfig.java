package com.Baluga.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@Configuration
public class RedisConfig {

    @Bean
    public DefaultRedisScript<Long> seckillStockScript() {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setResultType(Long.class);
        redisScript.setScriptText(
                "local stock = redis.call('DECR', KEYS[1]) " +
                "if stock < 0 then " +
                "    redis.call('INCR', KEYS[1]) " +
                "    return -1 " +
                "end " +
                "return stock"
        );
        return redisScript;
    }
}
