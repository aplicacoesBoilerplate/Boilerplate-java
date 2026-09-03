package com.java.boilerplate.service.helpers;

import com.java.boilerplate.exception.CExceptionsSystem;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.mockito.ArgumentCaptor;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CRateLimitRedisFallbackTests {
    @Test
    void deveRemoverJanelaRedisComAMesmaChaveOpacaDoLogin() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        CRateLimitService limiter = new CRateLimitService(100, redisTemplate);

        limiter.limpar("login:identidade", "usuario@example.com");

        ArgumentCaptor<String> chaveRemovida = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).delete(chaveRemovida.capture());
        assertThat(chaveRemovida.getValue()).isEqualTo(new CRedisRateLimitBackend(redisTemplate).chave(
                new CRateLimitService.RLimite("login:identidade", "usuario@example.com", 1, Duration.ofSeconds(60))
        ));
        assertThat(chaveRemovida.getValue()).doesNotContain("usuario@example.com");
    }

    @Test
    void retornoNuloDoScriptDeveCairNoFallbackLocal() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(), anyList(), any(Object[].class))).thenReturn(null);
        CRateLimitService limiter = new CRateLimitService(100, redisTemplate);

        limiter.consumir("api:subject", "anonymous:203.0.113.8", 1, Duration.ofSeconds(60));

        assertThatThrownBy(() -> limiter.consumir(
                "api:subject", "anonymous:203.0.113.8", 1, Duration.ofSeconds(60)))
                .isInstanceOf(CExceptionsSystem.class);
    }

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
