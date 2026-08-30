package com.java.boilerplate.cache;

import java.time.Duration;
import java.util.Optional;

/**
 * @description Contrato de cache opcional para dados que permanecem persistidos na fonte de verdade.
 */
public interface IRedisCache {
    Optional<String> obter(String pChave);

    void salvar(String pChave, String pValor, Duration pTtl);

    void remover(String pChave);
}
