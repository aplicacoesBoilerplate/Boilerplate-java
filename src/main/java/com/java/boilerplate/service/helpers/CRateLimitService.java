package com.java.boilerplate.service.helpers;

import com.java.boilerplate.config.RRateLimitProperties;
import com.java.boilerplate.exception.CExceptionsSystem;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@Service
public class CRateLimitService {
    private static final String MENSAGEM_LIMITE = "Muitas tentativas. Aguarde antes de tentar novamente";

    private final Map<String, CJanela> janelas = new ConcurrentHashMap<>();
    private final int maxTrackedKeys;
    private final CRedisRateLimitBackend redisBackend;

    @Autowired
    public CRateLimitService(
            RRateLimitProperties pProperties,
            ObjectProvider<StringRedisTemplate> pRedisTemplate,
            @Value("${redis.enabled:true}") boolean pRedisEnabled
    ) {
        this(pProperties.maxTrackedKeys(), pRedisEnabled ? pRedisTemplate.getIfAvailable() : null);
    }

    public CRateLimitService(int pMaxTrackedKeys) {
        this(pMaxTrackedKeys, null);
    }

    CRateLimitService(int pMaxTrackedKeys, StringRedisTemplate pRedisTemplate) {
        this.maxTrackedKeys = pMaxTrackedKeys;
        this.redisBackend = pRedisTemplate == null ? null : new CRedisRateLimitBackend(pRedisTemplate);
    }

    public void consumir(String pEscopo, String pIdentificador, int pLimite, Duration pJanela) {
        consumirTodos(List.of(new RLimite(pEscopo, pIdentificador, pLimite, pJanela)));
    }

    /**
     * Avalia e confirma todos os limites como uma unica decisao. Um pedido
     * rejeitado por IP ou identidade nao drena o contador global.
     */
    public void consumirTodos(List<RLimite> pLimites) {
        if (redisBackend != null) {
            try {
                redisBackend.consumirTodos(pLimites);
                return;
            } catch (CExceptionsSystem pException) {
                throw pException;
            } catch (RuntimeException pException) {
                // Redis é opcional: falhas transitórias seguem para a proteção local limitada.
            }
        }
        consumirTodosLocal(pLimites);
    }

    private synchronized void consumirTodosLocal(List<RLimite> pLimites) {
        if (pLimites == null || pLimites.isEmpty()) {
            throw new IllegalArgumentException("Ao menos um limite deve ser informado");
        }

        Instant agora = Instant.now();
        janelas.entrySet().removeIf(pEntry -> !agora.isBefore(pEntry.getValue().expiraEm()));

        Set<String> novasChaves = new HashSet<>();
        for (RLimite limite : pLimites) {
            validar(limite);
            String chave = chave(limite.escopo(), limite.identificador());
            if (!janelas.containsKey(chave)) {
                novasChaves.add(chave);
            }
        }
        if (janelas.size() + novasChaves.size() > maxTrackedKeys) {
            throw limiteExcedido(agora, agora.plusSeconds(1));
        }

        Map<String, CJanela> proximas = new HashMap<>();
        for (RLimite limite : pLimites) {
            String chave = chave(limite.escopo(), limite.identificador());
            CJanela atual = proximas.getOrDefault(chave, janelas.get(chave));
            if (atual == null || !agora.isBefore(atual.expiraEm())) {
                atual = new CJanela(0, agora.plus(limite.janela()));
            }
            if (atual.tentativas() >= limite.limite()) {
                throw limiteExcedido(agora, atual.expiraEm());
            }
            proximas.put(chave, new CJanela(atual.tentativas() + 1, atual.expiraEm()));
        }
        janelas.putAll(proximas);
    }

    private void validar(RLimite pLimite) {
        if (pLimite == null || pLimite.limite() < 1 || pLimite.janela() == null
                || pLimite.janela().isZero() || pLimite.janela().isNegative()) {
            throw new IllegalArgumentException("Configuração de limite inválida");
        }
    }

    private String chave(String pEscopo, String pIdentificador) {
        return CHashUtil.gerarSha256(pEscopo + ":" + normalizar(pIdentificador));
    }

    public void limpar(String pEscopo, String pIdentificador) {
        janelas.remove(chave(pEscopo, pIdentificador));
        if (redisBackend != null) {
            try {
                redisBackend.limpar(pEscopo, pIdentificador);
            } catch (RuntimeException pException) {
                // A limpeza local continua válida quando Redis está temporariamente indisponível.
            }
        }
    }

    private String normalizar(String pIdentificador) {
        return pIdentificador == null ? "desconhecido" : pIdentificador.trim().toLowerCase();
    }

    private CExceptionsSystem limiteExcedido(Instant pAgora, Instant pExpiraEm) {
        long millis = Math.max(1, Duration.between(pAgora, pExpiraEm).toMillis());
        long segundos = Math.max(1, (millis + 999) / 1_000);
        return new CExceptionsSystem(MENSAGEM_LIMITE, HttpStatus.TOO_MANY_REQUESTS, Math.toIntExact(segundos));
    }

    private record CJanela(int tentativas, Instant expiraEm) {
    }

    public record RLimite(String escopo, String identificador, int limite, Duration janela) {
    }
}
