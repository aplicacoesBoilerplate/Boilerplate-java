package com.java.boilerplate.service.helpers;

import com.java.boilerplate.exception.CExceptionsSystem;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CRateLimitRedisFallbackTests {
    @Test
    void deveManterLimiteLocalQuandoRedisEstaIndisponivel() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(), anyList(), any(Object[].class)))
                .thenThrow(new DataAccessResourceFailureException("offline"));
        CRateLimitService limiter = new CRateLimitService(100, redisTemplate);

        limiter.consumir("api:subject", "anonymous:203.0.113.9", 1, Duration.ofSeconds(60));

        assertThatThrownBy(() -> limiter.consumir(
                "api:subject", "anonymous:203.0.113.9", 1, Duration.ofSeconds(60)))
                .isInstanceOf(CExceptionsSystem.class)
                .hasFieldOrPropertyWithValue("status", org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
    }
}
