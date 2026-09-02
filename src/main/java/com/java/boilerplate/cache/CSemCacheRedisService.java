package com.java.boilerplate.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * @description Implementacao sem efeito usada quando o cache Redis esta desabilitado por configuracao.
 */
public class CSemCacheRedisService implements IRedisCache {
    @Override
    public Optional<String> obter(String pChave) {
        return Optional.empty();
    }

    @Override
    public void salvar(String pChave, String pValor, Duration pTtl) {}

    @Override
    public void salvarPermanente(String pChave, String pValor) {}

    @Override
    public void remover(String pChave) {}
}
