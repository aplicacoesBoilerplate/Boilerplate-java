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
    private final CPublicRequestSecurityFilter filter = new CPublicRequestSecurityFilter(
            new CRateLimitService(100),
            new RRateLimitProperties(60, 100, 100, 10, 5, 10_000)
    );

    @Test
    void corpoPublicoAcimaDe64KiBDeveSerRejeitadoAntesDoMvc() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request("POST", "/auth/login", "x".repeat(64 * 1024 + 1)), response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void corpoAutenticadoAcimaDe64KiBTambemDeveSerRejeitadoAntesDoMvc() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request("PUT", "/preferencias/me", "x".repeat(64 * 1024 + 1)), response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void corpoAte64KiBDevePassarNormalmente() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request("POST", "/auth/login", "{}"), response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void metodoSemCorpoDevePassarSemValidacao() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health-check/public");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void rotaNaoPublicaSemCorpoDevePassar() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/usuarios");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    void probeLoopbackDevePassarNormalmente() throws Exception {
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

    private MockHttpServletRequest request(String pMethod, String pPath, String pBody) {
        MockHttpServletRequest request = new MockHttpServletRequest(pMethod, pPath);
        request.setRemoteAddr("203.0.113.1");
        request.setContentType("application/json");
        request.setContent(pBody.getBytes(StandardCharsets.UTF_8));
        return request;
    }
}
