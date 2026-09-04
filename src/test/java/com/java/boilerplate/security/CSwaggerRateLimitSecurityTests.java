package com.java.boilerplate.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "documentacao.enabled=true",
        "security.rate-limit.global-requests-per-window=2",
        "security.rate-limit.login-attempts-per-window=20"
})
@AutoConfigureMockMvc
class CSwaggerRateLimitSecurityTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void tentativasBasicInvalidasDevemSerLimitadasNaCadeiaSwaggerSemDuplicarGlobal() throws Exception {
        for (int tentativa = 0; tentativa < 2; tentativa++) {
            mockMvc.perform(get("/api/v1/doc")
                            .servletPath("/api/v1")
                            .with(httpBasic("test-doc-user", "senha-invalida")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE));
        }

        mockMvc.perform(get("/api/v1/doc")
                        .servletPath("/api/v1")
                        .with(httpBasic("test-doc-user", "senha-invalida")))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"));
    }
}
