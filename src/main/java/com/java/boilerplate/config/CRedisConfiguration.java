package com.java.boilerplate.config;

import com.java.boilerplate.cache.CRedisCacheService;
import com.java.boilerplate.cache.CSemCacheRedisService;
import com.java.boilerplate.cache.IRedisCache;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @description Expõe um cache Redis opcional sobre a conexao Lettuce unica gerenciada pelo Spring Boot.
 */
@Configuration
public class CRedisConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "redis", name = "enabled", havingValue = "true", matchIfMissing = true)
    public IRedisCache redisCache(StringRedisTemplate pRedisTemplate) {
        return new CRedisCacheService(pRedisTemplate);
    }

    @Bean
    @ConditionalOnProperty(prefix = "redis", name = "enabled", havingValue = "false")
    public IRedisCache semCacheRedis() {
        return new CSemCacheRedisService();
    }
}
