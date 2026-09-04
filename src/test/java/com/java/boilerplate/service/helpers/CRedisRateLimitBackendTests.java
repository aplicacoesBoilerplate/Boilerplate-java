package com.java.boilerplate.service.helpers;

import com.java.boilerplate.exception.CExceptionsSystem;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CRedisRateLimitBackendTests {

    @Test
    void deveArredondarRetryAfterParaCimaQuandoOLuaBloqueia() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(), anyList(), any(Object[].class)))
                .thenReturn(1_001L);
        CRedisRateLimitBackend backend = new CRedisRateLimitBackend(redisTemplate);

        assertThatThrownBy(() -> backend.consumirTodos(List.of(
                new CRateLimitService.RLimite("api", "global", 10, Duration.ofSeconds(60)),
                new CRateLimitService.RLimite("api", "subject", 5, Duration.ofSeconds(60))
        )))
                .isInstanceOf(CExceptionsSystem.class)
                .satisfies(pException -> assertThat(((CExceptionsSystem) pException).getRetryAfterSeconds()).isEqualTo(2));
    }
}
