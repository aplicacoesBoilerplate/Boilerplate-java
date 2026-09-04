package com.java.boilerplate.service.helpers;

import com.java.boilerplate.exception.CExceptionsSystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(named = "REDIS_INTEGRATION_ENABLED", matches = "true")
class CRedisRateLimitIntegrationTests {
    private final LettuceConnectionFactory connectionFactory = new LettuceConnectionFactory(
            new RedisStandaloneConfiguration(System.getenv().getOrDefault("REDIS_HOST", "localhost"),
                    Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379")))
    );
    private final StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);

    CRedisRateLimitIntegrationTests() {
        connectionFactory.afterPropertiesSet();
        redisTemplate.afterPropertiesSet();
    }

    @AfterEach
    void encerrar() {
        connectionFactory.destroy();
    }

    @Test
    void deveCompartilharCotasAtomicamenteEntreFachadasConcorrentes() throws Exception {
        CRedisRateLimitBackend primeiraFachada = new CRedisRateLimitBackend(redisTemplate);
        CRedisRateLimitBackend segundaFachada = new CRedisRateLimitBackend(redisTemplate);
        String escopo = "integration-" + System.nanoTime();
        CountDownLatch inicio = new CountDownLatch(1);
        CountDownLatch fim = new CountDownLatch(10);
        ConcurrentLinkedQueue<String> sucessos = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<Throwable> falhasInesperadas = new ConcurrentLinkedQueue<>();

        for (int indice = 0; indice < 10; indice++) {
            String sujeito = indice % 2 == 0 ? "user:a" : "user:b";
            CRedisRateLimitBackend fachada = indice % 2 == 0 ? primeiraFachada : segundaFachada;
            Thread thread = new Thread(() -> {
                try {
                    inicio.await();
                    fachada.consumirTodos(limites(escopo, sujeito, 5, 3));
                    sucessos.add(sujeito);
                } catch (CExceptionsSystem ignored) {
                    // Rejeição esperada após esgotamento das cotas compartilhadas.
                } catch (Throwable pException) {
                    falhasInesperadas.add(pException);
                } finally {
                    fim.countDown();
                }
            });
            thread.start();
        }
        inicio.countDown();
        assertThat(fim.await(10, TimeUnit.SECONDS)).isTrue();

        assertThat(falhasInesperadas).isEmpty();
        assertThat(sucessos).hasSize(5);
        assertThat(sucessos.stream().filter("user:a"::equals).count()).isLessThanOrEqualTo(3);
        assertThat(sucessos.stream().filter("user:b"::equals).count()).isLessThanOrEqualTo(3);
    }

    @Test
    void rejeicaoDeSujeitoNaoDeveDrenarQuotaGlobal() {
        CRedisRateLimitBackend backend = new CRedisRateLimitBackend(redisTemplate);
        String escopo = "integration-no-drain-" + System.nanoTime();
        backend.consumirTodos(limites(escopo, "user:a", 2, 1));

        try {
            backend.consumirTodos(limites(escopo, "user:a", 2, 1));
        } catch (CExceptionsSystem ignored) {
            // sujeito A bloqueado
        }

        backend.consumirTodos(limites(escopo, "user:b", 2, 1));
    }

    private List<CRateLimitService.RLimite> limites(String pEscopo, String pSujeito, int pGlobal, int pSubject) {
        Duration janela = Duration.ofSeconds(30);
        return List.of(
                new CRateLimitService.RLimite(pEscopo + ":global", "global", pGlobal, janela),
                new CRateLimitService.RLimite(pEscopo + ":subject", pSujeito, pSubject, janela)
        );
    }
}
