package com.java.boilerplate.security;

import com.java.boilerplate.config.RRateLimitProperties;
import com.java.boilerplate.config.security.CApiRateLimitSecurityFilter;
import com.java.boilerplate.service.helpers.CRateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CApiRateLimitSecurityFilterTests {
    @Test
    void deveLimitarTodaRotaApiAnonimaComResposta429ERetryAfter() throws Exception {
        CApiRateLimitSecurityFilter filter = new CApiRateLimitSecurityFilter(
                new CRateLimitService(100),
                new RRateLimitProperties(60, 100, 100, 1, 5, 100)
        );
        MockHttpServletRequest primeira = request("/api/v1/auth/cadastro", "203.0.113.11");
        filter.doFilter(primeira, new MockHttpServletResponse(), new MockFilterChain());

        MockHttpServletResponse resposta = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request("/api/v1/auth/cadastro", "203.0.113.11"), resposta, chain);

        assertThat(resposta.getStatus()).isEqualTo(429);
        assertThat(resposta.getHeader("Retry-After")).isEqualTo("60");
        assertThat(chain.getRequest()).isNull();
    }

    private MockHttpServletRequest request(String pPath, String pRemoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", pPath);
        request.setRemoteAddr(pRemoteAddr);
        return request;
    }
}
