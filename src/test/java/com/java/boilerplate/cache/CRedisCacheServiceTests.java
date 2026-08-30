package com.java.boilerplate.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class CRedisCacheServiceTests {
    @Test
    void obterRetornaValorDisponivelNoRedis() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = criarValueOperations();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("cache:teste")).thenReturn("valor");

        IRedisCache cache = new CRedisCacheService(redisTemplate);

        assertEquals("valor", cache.obter("cache:teste").orElseThrow());
    }

    @Test
    void salvarIgnoraFalhaDeInfraestruturaRedis() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = criarValueOperations();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        org.mockito.Mockito.doThrow(new DataAccessResourceFailureException("Redis indisponivel"))
                .when(valueOperations)
                .set("cache:teste", "valor", Duration.ofMinutes(1));

        IRedisCache cache = new CRedisCacheService(redisTemplate);

        cache.salvar("cache:teste", "valor", Duration.ofMinutes(1));

        verify(valueOperations).set("cache:teste", "valor", Duration.ofMinutes(1));
    }

    @Test
    void cacheDesabilitadoSempreRetornaAusencia() {
        IRedisCache cache = new CSemCacheRedisService();

        cache.salvar("cache:teste", "valor", Duration.ofMinutes(1));
        cache.remover("cache:teste");

        assertTrue(cache.obter("cache:teste").isEmpty());
    }

    @SuppressWarnings("unchecked")
    private ValueOperations<String, String> criarValueOperations() {
        return mock(ValueOperations.class);
    }
}
