package com.java.boilerplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.java.boilerplate.integration.softwarecenter.ISoftwareCenterClient;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RContextoSessao;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RCriarSessaoGoogle;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RCriarSessaoSenha;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RSessaoCriada;
import com.java.boilerplate.integration.softwarecenter.RSoftwareCenterDtos.RSessaoValidada;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(CBoilerplateApplicationTests.CConfiguracaoTeste.class)
class CBoilerplateApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CClienteSoftwareCenterTeste softwareCenterClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void prepararClienteSoftwareCenter() {
        softwareCenterClient.resetar();
    }

    @Test
    void contextLoads() {}

    @Test
    void loginBffCriaSessaoCookieEContextoSemToken() throws Exception {
        RRespostaCsrfTeste csrf = obterCsrf();

        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
							{
							  "email": "usuario@empresa.com",
							  "password": "senha-nao-exposta",
							  "tenantSubdominio": "empresa"
							}
							"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(10))
                .andExpect(jsonPath("$.permissions[0]").value("boilerplate.usuarios.read"))
                .andExpect(jsonPath("$.sessionId").doesNotExist())
                .andExpect(jsonPath("$.tokenJWT").doesNotExist())
                .andReturn();

        Cookie sessionCookie = resultado.getResponse().getCookie("BOILERPLATE_SESSION");
        assertNotNull(sessionCookie);
        List<String> cookies = resultado.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        assertTrue(cookies.stream()
                .anyMatch(pCookie -> pCookie.contains("BOILERPLATE_SESSION")
                        && pCookie.contains("HttpOnly")
                        && pCookie.contains("SameSite=Lax")));
    }

    @Test
    void consultaSessaoRevalidaContextoNaSoftwareCenter() throws Exception {
        Cookie sessionCookie = autenticar();

        mockMvc.perform(get("/api/v1/auth/session").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantSubdomain").value("empresa"))
                .andExpect(jsonPath("$.roleName").value("Administrador"));

        assertEquals(1, softwareCenterClient.quantidadeRevalidacoes());
    }

    @Test
    void mutacaoSemCsrfRecebeForbidden() throws Exception {
        Cookie sessionCookie = autenticar();

        mockMvc.perform(post("/usuarios/consulta")
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutInvalidaSessaoLocalMesmoQuandoRevogacaoRemotaFalha() throws Exception {
        Cookie sessionCookie = autenticar();
        RRespostaCsrfTeste csrf = obterCsrf();
        softwareCenterClient.falharNaRevogacao();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(sessionCookie, csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token()))
                .andExpect(status().isNoContent());

        assertEquals(1, softwareCenterClient.quantidadeRevogacoes());
        mockMvc.perform(get("/api/v1/auth/session").cookie(sessionCookie)).andExpect(status().isUnauthorized());
    }

    private Cookie autenticar() throws Exception {
        RRespostaCsrfTeste csrf = obterCsrf();
        MvcResult resultado = mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrf.cookie())
                        .header("X-XSRF-TOKEN", csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
							{
							  "email": "usuario@empresa.com",
							  "senha": "senha-nao-exposta",
							  "tenantSubdominio": "empresa"
							}
							"""))
                .andExpect(status().isOk())
                .andReturn();
        Cookie sessionCookie = resultado.getResponse().getCookie("BOILERPLATE_SESSION");
        assertNotNull(sessionCookie);
        return sessionCookie;
    }

    private RRespostaCsrfTeste obterCsrf() throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode resposta = objectMapper.readTree(resultado.getResponse().getContentAsString());
        Cookie cookie = resultado.getResponse().getCookie("BOILERPLATE-XSRF-TOKEN");
        assertNotNull(cookie);
        return new RRespostaCsrfTeste(resposta.path("token").asText(), cookie);
    }

    private record RRespostaCsrfTeste(String token, Cookie cookie) {}

    @TestConfiguration(proxyBeanMethods = false)
    static class CConfiguracaoTeste {
        @Bean
        @Primary
        CClienteSoftwareCenterTeste softwareCenterClientTeste() {
            return new CClienteSoftwareCenterTeste();
        }
    }

    static class CClienteSoftwareCenterTeste implements ISoftwareCenterClient {
        private int revalidacoes;
        private int revogacoes;
        private boolean falharRevogacao;

        @Override
        public RSessaoCriada criarSessaoComSenha(RCriarSessaoSenha pComando) {
            return criarSessao();
        }

        @Override
        public RSessaoCriada criarSessaoComGoogle(RCriarSessaoGoogle pComando) {
            return criarSessao();
        }

        @Override
        public RSessaoValidada revalidarSessao(String pSessionId) {
            revalidacoes++;
            return new RSessaoValidada(Instant.parse("2030-01-01T00:00:00Z"), criarContexto());
        }

        @Override
        public void revogarSessao(String pSessionId) {
            revogacoes++;
            if (falharRevogacao) {
                throw new IllegalStateException("falha remota simulada");
            }
        }

        @Override
        public void cadastrar(RSoftwareCenterDtos.RCadastro pComando) {}

        @Override
        public void solicitarRecuperacao(RSoftwareCenterDtos.RSolicitarRecuperacao pComando) {}

        @Override
        public void verificarRecuperacao(RSoftwareCenterDtos.RVerificarRecuperacao pComando) {}

        @Override
        public void redefinirSenha(RSoftwareCenterDtos.RRedefinirSenha pComando) {}

        void resetar() {
            revalidacoes = 0;
            revogacoes = 0;
            falharRevogacao = false;
        }

        void falharNaRevogacao() {
            falharRevogacao = true;
        }

        int quantidadeRevalidacoes() {
            return revalidacoes;
        }

        int quantidadeRevogacoes() {
            return revogacoes;
        }

        private RSessaoCriada criarSessao() {
            return new RSessaoCriada("sessao-opaca-de-teste", Instant.parse("2030-01-01T00:00:00Z"), criarContexto());
        }

        private RContextoSessao criarContexto() {
            return new RContextoSessao(
                    10L,
                    20L,
                    "empresa",
                    30L,
                    40L,
                    50L,
                    "Administrador",
                    "mdi-shield-account",
                    Set.of("boilerplate.usuarios.read"),
                    Set.of("RBAC_MANAGE"));
        }
    }
}
