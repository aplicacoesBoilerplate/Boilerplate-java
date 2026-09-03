package com.java.boilerplate.service.helpers;

import com.java.boilerplate.exception.CExceptionsSystem;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Executa a decisão de rate limit em um único script Redis atômico. */
public class CRedisRateLimitBackend {
    private static final String MENSAGEM_LIMITE = "Muitas tentativas. Aguarde antes de tentar novamente";
    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = criarScript();

    private final StringRedisTemplate redisTemplate;

    public CRedisRateLimitBackend(StringRedisTemplate pRedisTemplate) {
        this.redisTemplate = pRedisTemplate;
    }

    public void consumirTodos(List<CRateLimitService.RLimite> pLimites) {
        validar(pLimites);
        List<String> chaves = new ArrayList<>();
        List<String> argumentos = new ArrayList<>();
        long janelaMillis = pLimites.get(0).janela().toMillis();
        for (CRateLimitService.RLimite limite : pLimites) {
            if (limite.janela().toMillis() != janelaMillis) {
                throw new IllegalArgumentException("Todos os limites devem usar a mesma janela Redis");
            }
            chaves.add(chave(limite));
            argumentos.add(String.valueOf(limite.limite()));
        }
        argumentos.add(String.valueOf(janelaMillis));

        Object[] argumentosDoScript = argumentos.toArray(String[]::new);
        Long bloqueioMillis = redisTemplate.execute(RATE_LIMIT_SCRIPT, chaves, argumentosDoScript);
        if (bloqueioMillis != null && bloqueioMillis > 0) {
            long retryAfter = Math.max(1, (bloqueioMillis + 999) / 1_000);
            throw new CExceptionsSystem(MENSAGEM_LIMITE, HttpStatus.TOO_MANY_REQUESTS, Math.toIntExact(retryAfter));
        }
    }

    String chave(CRateLimitService.RLimite pLimite) {
        return "boilerplate:rate-limit:{api}:" + CHashUtil.gerarSha256(pLimite.escopo())
                + ":" + CHashUtil.gerarSha256(normalizar(pLimite.identificador()));
    }

    private void validar(List<CRateLimitService.RLimite> pLimites) {
        if (pLimites == null || pLimites.isEmpty()) {
            throw new IllegalArgumentException("Ao menos um limite deve ser informado");
        }
        for (CRateLimitService.RLimite limite : pLimites) {
            if (limite == null || limite.limite() < 1 || limite.janela() == null
                    || limite.janela().isNegative() || limite.janela().isZero()
                    || limite.janela().toMillis() < 1) {
                throw new IllegalArgumentException("Configuração de limite inválida");
            }
        }
    }

    private String normalizar(String pIdentificador) {
        return pIdentificador == null ? "desconhecido" : pIdentificador.trim().toLowerCase();
    }

    private static DefaultRedisScript<Long> criarScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                local ttl = tonumber(ARGV[#ARGV])
                local maiorPttl = 0
                for indice = 1, #KEYS do
                  local contador = tonumber(redis.call('GET', KEYS[indice]) or '0')
                  local pttl = redis.call('PTTL', KEYS[indice])
                  if contador > 0 and pttl <= 0 then
                    redis.call('PEXPIRE', KEYS[indice], ttl)
                    pttl = ttl
                  end
                  if contador >= tonumber(ARGV[indice]) and pttl > maiorPttl then
                    maiorPttl = pttl
                  end
                end
                if maiorPttl > 0 then
                  return maiorPttl
                end
                for indice = 1, #KEYS do
                  local contador = redis.call('INCR', KEYS[indice])
                  if contador == 1 or redis.call('PTTL', KEYS[indice]) <= 0 then
                    redis.call('PEXPIRE', KEYS[indice], ttl)
                  end
                end
                return 0
                """);
        return script;
    }
}
