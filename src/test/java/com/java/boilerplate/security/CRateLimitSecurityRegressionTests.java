package com.java.boilerplate.security;

import com.java.boilerplate.exception.CExceptionsSystem;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.List;
import com.java.boilerplate.service.helpers.CRateLimitService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CRateLimitSecurityRegressionTests {
    @Test
    void caminhoRedisSaudavelNaoDeveSerializarChamadasNoServico() throws Exception {
        int modificadores = CRateLimitService.class
                .getMethod("consumirTodos", List.class)
                .getModifiers();

        assertThat(Modifier.isSynchronized(modificadores)).isFalse();
    }

    @Test
    void pedidoBloqueadoPorIpNaoDeveConsumirQuotaGlobal() {
        CRateLimitService limiter = new CRateLimitService(100);
        Duration janela = Duration.ofMinutes(1);

        limiter.consumirTodos(List.of(
                new CRateLimitService.RLimite("rota:ip", "ip-a", 1, janela),
                new CRateLimitService.RLimite("rota:global", "global", 2, janela)
        ));

        assertThatThrownBy(() -> limiter.consumirTodos(List.of(
                new CRateLimitService.RLimite("rota:ip", "ip-a", 1, janela),
                new CRateLimitService.RLimite("rota:global", "global", 2, janela)
        ))).isInstanceOf(CExceptionsSystem.class);

        limiter.consumirTodos(List.of(
                new CRateLimitService.RLimite("rota:ip", "ip-b", 1, janela),
                new CRateLimitService.RLimite("rota:global", "global", 2, janela)
        ));
    }

    @Test
    void excederLimiteDeveFalharFechadoComStatus429() throws Exception {
        Class<?> type = Class.forName("com.java.boilerplate.service.helpers.CRateLimitService");
        Object limiter = type.getConstructor(int.class).newInstance(100);
        var consumir = type.getMethod("consumir", String.class, String.class, int.class, Duration.class);

        consumir.invoke(limiter, "login", "conta@example.com", 2, Duration.ofMinutes(1));
        consumir.invoke(limiter, "login", "conta@example.com", 2, Duration.ofMinutes(1));

        assertThatThrownBy(() -> consumir.invoke(
                limiter,
                "login",
                "conta@example.com",
                2,
                Duration.ofMinutes(1)
        ))
                .isInstanceOf(InvocationTargetException.class)
                .satisfies(pErro -> assertThat(((InvocationTargetException) pErro).getCause())
                        .isInstanceOf(CExceptionsSystem.class)
                        .extracting(pCause -> ((CExceptionsSystem) pCause).getStatus().value())
                        .isEqualTo(429));
    }

    @Test
    void decisaoMultidimensionalNaoDeveUltrapassarCapacidadeDeChaves() {
        CRateLimitService limiter = new CRateLimitService(1);
        Duration janela = Duration.ofMinutes(1);

        assertThatThrownBy(() -> limiter.consumirTodos(List.of(
                new CRateLimitService.RLimite("rota:ip", "ip-a", 1, janela),
                new CRateLimitService.RLimite("rota:global", "global", 1, janela)
        ))).isInstanceOf(CExceptionsSystem.class);

        limiter.consumir("rota:ip", "ip-b", 1, janela);
    }
}
