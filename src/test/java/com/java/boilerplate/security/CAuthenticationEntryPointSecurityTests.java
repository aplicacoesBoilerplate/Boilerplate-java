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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    void preflightDeveAceitarOrigemEAuthorizationDaSpaLocal() throws Exception {
        mockMvc.perform(options("/api/v1/auth/me")
                        .servletPath("/api/v1")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    void solicitacaoDeRecuperacaoPublicaNaoDeveExigirTokenCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/auth/recuperacao-senha/solicitar")
                        .servletPath("/api/v1")
                        .contentType("application/json")
                        .content("{\"email\":\"inexistente@example.com\"}"))
                .andExpect(status().isAccepted());
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
