package com.java.boilerplate.security;

import com.java.boilerplate.config.RRateLimitProperties;
import com.java.boilerplate.config.security.CPublicRequestSecurityFilter;
import com.java.boilerplate.service.helpers.CRateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CPublicRequestSecurityFilterTests {
    private final RRateLimitProperties properties = new RRateLimitProperties(60, 1, 1, 10_000);
    private final CPublicRequestSecurityFilter filter = new CPublicRequestSecurityFilter(
            new CRateLimitService(100),
            properties
    );

    @Test
    void corpoPublicoAcimaDe64KiBDeveSerRejeitadoAntesDoMvc() throws Exception {
        MockHttpServletRequest request = requestLogin("x".repeat(64 * 1024 + 1), "127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void corpoAutenticadoAcimaDe64KiBTambemDeveSerRejeitadoAntesDoMvc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/preferencias/me");
        request.setContentType("application/json");
        request.setContent("x".repeat(64 * 1024 + 1).getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void excessoPorIpDeveRetornar429ComRetryAfter() throws Exception {
        MockHttpServletResponse primeiraResposta = new MockHttpServletResponse();
        filter.doFilter(requestLogin("{}", "127.0.0.2"), primeiraResposta, new MockFilterChain());

        MockHttpServletResponse segundaResposta = new MockHttpServletResponse();
        MockFilterChain segundaChain = new MockFilterChain();
        filter.doFilter(requestLogin("{}", "127.0.0.2"), segundaResposta, segundaChain);

        assertThat(segundaResposta.getStatus()).isEqualTo(429);
        assertThat(segundaResposta.getHeader("Retry-After")).isNotBlank();
        assertThat(segundaChain.getRequest()).isNull();
    }

    @Test
    void autenticacaoBasicDaDocumentacaoDeveTerLimiteAntesDoBcrypt() throws Exception {
        MockHttpServletRequest primeira = new MockHttpServletRequest("GET", "/doc");
        primeira.setRemoteAddr("127.0.0.3");
        primeira.addHeader("Authorization", "Basic credencial-invalida");
        filter.doFilter(primeira, new MockHttpServletResponse(), new MockFilterChain());

        MockHttpServletRequest segunda = new MockHttpServletRequest("GET", "/doc");
        segunda.setRemoteAddr("127.0.0.3");
        segunda.addHeader("Authorization", "Basic credencial-invalida");
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(segunda, resposta, chain);

        assertThat(resposta.getStatus()).isEqualTo(429);
        assertThat(resposta.getHeader("Retry-After")).isNotBlank();
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void healthPublicoDeveTerLimitePorIpAntesDoActuator() throws Exception {
        MockHttpServletRequest primeira = new MockHttpServletRequest("GET", "/actuator/health-check/public");
        primeira.setRemoteAddr("203.0.113.4");
        filter.doFilter(primeira, new MockHttpServletResponse(), new MockFilterChain());

        MockHttpServletRequest segunda = new MockHttpServletRequest("GET", "/actuator/health-check/public");
        segunda.setRemoteAddr("203.0.113.4");
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(segunda, resposta, chain);

        assertThat(resposta.getStatus()).isEqualTo(429);
        assertThat(resposta.getHeader("Retry-After")).isNotBlank();
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void headDoHealthPublicoTambemDeveConsumirLimite() throws Exception {
        MockHttpServletRequest primeira = new MockHttpServletRequest("HEAD", "/actuator/health-check/public");
        primeira.setRemoteAddr("203.0.113.5");
        filter.doFilter(primeira, new MockHttpServletResponse(), new MockFilterChain());

        MockHttpServletRequest segunda = new MockHttpServletRequest("HEAD", "/actuator/health-check/public");
        segunda.setRemoteAddr("203.0.113.5");
        MockHttpServletResponse resposta = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(segunda, resposta, chain);

        assertThat(resposta.getStatus()).isEqualTo(429);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void probeLoopbackNaoDeveCompartilharQuotaExterna() throws Exception {
        for (int tentativa = 0; tentativa < 5; tentativa++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health-check/public");
            request.setRemoteAddr("127.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(chain.getRequest()).isNotNull();
        }
    }

    @Test
    void loopbackNaoDeveIsentarLoginDaQuotaPublica() throws Exception {
        filter.doFilter(
                requestLogin("{}", "127.0.0.1"),
                new MockHttpServletResponse(),
                new MockFilterChain()
        );

        MockHttpServletResponse segundaResposta = new MockHttpServletResponse();
        MockFilterChain segundaChain = new MockFilterChain();
        filter.doFilter(requestLogin("{}", "127.0.0.1"), segundaResposta, segundaChain);

        assertThat(segundaResposta.getStatus()).isEqualTo(429);
        assertThat(segundaChain.getRequest()).isNull();
    }

    @Test
    void loginVersionadoDeveManterALimitacaoPublicaPorIp() throws Exception {
        filter.doFilter(
                requestLogin("/api/v1/auth/login", "{}", "127.0.0.4"),
                new MockHttpServletResponse(),
                new MockFilterChain()
        );

        MockHttpServletResponse segundaResposta = new MockHttpServletResponse();
        MockFilterChain segundaChain = new MockFilterChain();
        filter.doFilter(
                requestLogin("/api/v1/auth/login", "{}", "127.0.0.4"),
                segundaResposta,
                segundaChain
        );

        assertThat(segundaResposta.getStatus()).isEqualTo(429);
        assertThat(segundaResposta.getHeader("Retry-After")).isNotBlank();
        assertThat(segundaChain.getRequest()).isNull();
    }

    private MockHttpServletRequest requestLogin(String pBody, String pIp) {
        return requestLogin("/auth/login", pBody, pIp);
    }

    private MockHttpServletRequest requestLogin(String pPath, String pBody, String pIp) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", pPath);
        request.setRemoteAddr(pIp);
        request.setContentType("application/json");
        request.setContent(pBody.getBytes(StandardCharsets.UTF_8));
        return request;
    }
}
