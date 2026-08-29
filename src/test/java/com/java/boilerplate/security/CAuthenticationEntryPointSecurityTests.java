package com.java.boilerplate.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(CAuthenticationEntryPointSecurityTests.CConfiguracaoTeste.class)
class CAuthenticationEntryPointSecurityTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void recursoProtegidoSemSessaoValidaDeveSolicitarBearerCom401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .servletPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-revogado-ou-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
    }

    @Test
    void healthPublicoMinimoDevePermanecerDisponivelSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/actuator/health-check/public").servletPath("/api/v1"))
                .andExpect(status().isOk());
    }

    @Test
    void healthCompletoDeveExigirAdministrador() throws Exception {
        mockMvc.perform(get("/api/v1/actuator/health-check").servletPath("/api/v1"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
    }

    @Test
    void healthSmtpDeveExigirAdministrador() throws Exception {
        mockMvc.perform(get("/api/v1/actuator/health-check/smtp").servletPath("/api/v1"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administradorDeveConsultarIndicadorSmtpIsolado() throws Exception {
        mockMvc.perform(get("/api/v1/actuator/health-check/smtp").servletPath("/api/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.mail").exists());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CConfiguracaoTeste {
        @Bean
        @Primary
        JavaMailSenderImpl mailSender() {
            return new JavaMailSenderImpl() {
                @Override
                public void testConnection() {}
            };
        }
    }
}
