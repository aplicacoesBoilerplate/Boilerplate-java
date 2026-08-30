package com.java.boilerplate.cache;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @description Implementacao de cache Redis que degrada para cache ausente quando a infraestrutura falha.
 */
public class CRedisCacheService implements IRedisCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(CRedisCacheService.class);

    private final StringRedisTemplate redisTemplate;
    private final AtomicBoolean indisponivel = new AtomicBoolean(false);

    public CRedisCacheService(StringRedisTemplate pRedisTemplate) {
        this.redisTemplate = pRedisTemplate;
    }

    @Override
    public Optional<String> obter(String pChave) {
        try {
            String valor = redisTemplate.opsForValue().get(pChave);
            registrarRecuperacao();
            return Optional.ofNullable(valor);
        } catch (DataAccessException pException) {
            registrarIndisponibilidade(pException);
            return Optional.empty();
        }
    }

    @Override
    public void salvar(String pChave, String pValor, Duration pTtl) {
        try {
            redisTemplate.opsForValue().set(pChave, pValor, pTtl);
            registrarRecuperacao();
        } catch (DataAccessException pException) {
            registrarIndisponibilidade(pException);
        }
    }

    @Override
    public void remover(String pChave) {
        try {
            redisTemplate.delete(pChave);
            registrarRecuperacao();
        } catch (DataAccessException pException) {
            registrarIndisponibilidade(pException);
        }
    }

    private void registrarIndisponibilidade(DataAccessException pException) {
        if (indisponivel.compareAndSet(false, true)) {
            LOGGER.warn("Redis indisponivel; operacoes de cache serao ignoradas: {}", pException.getMessage());
        }
    }

    private void registrarRecuperacao() {
        if (indisponivel.compareAndSet(true, false)) {
            LOGGER.info("Redis recuperado; operacoes de cache foram retomadas");
        }
    }
}
