package com.java.boilerplate.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.java.boilerplate.model.CPreferenciaUsuario;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.IPreferenciaUsuarioRepository;
import com.java.boilerplate.repository.IUsuarioRepository;
import com.java.boilerplate.service.CPreferenciaUsuarioService;
import com.java.boilerplate.service.CUsuarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "bootstrap.admin.enabled=true",
        "bootstrap.admin.email=admin-preferencias@example.com",
        "bootstrap.admin.password=senha-admin-preferencias-segura",
        "bootstrap.admin.name=ADMIN PREFERENCIAS"
})
@AutoConfigureMockMvc
class CPreferenciaUsuarioControllerTests {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CPreferenciaUsuarioService preferenciaUsuarioService;
    @Autowired
    private CUsuarioService usuarioService;
    @Autowired
    private IUsuarioRepository usuarioRepository;
    @Autowired
    private IPreferenciaUsuarioRepository preferenciaUsuarioRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String token;

    @BeforeEach
    void autenticar() throws Exception {
        MvcResult resultadoLogin = mockMvc.perform(post("/api/v1/auth/token/login")
                        .servletPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identificacaoAcesso":"admin-preferencias@example.com","senha":"senha-admin-preferencias-segura"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode resposta = objectMapper.readTree(resultadoLogin.getResponse().getContentAsString());
        token = resposta.required("tokenJWT").asText();
    }

    @Test
    void deveRemoverPelaRotaVersionadaComParametrosEAceitarRepeticao() throws Exception {
        executarRemocao("filters", "usuarios")
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_TYPE));

        executarRemocao("filters", "usuarios")
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_TYPE));
    }

    @Test
    void remocaoIntegradaDeveConfirmarCommitEIsolamentoEntreUsuarios() throws Exception {
        assertThat(AopUtils.isAopProxy(preferenciaUsuarioService)).isTrue();
        CUsuario usuarioA = usuarioRepository.findByEmailIgnoreCase("admin-preferencias@example.com").orElseThrow();
        CUsuario usuarioB = usuarioService.criarUsuarioSistema(
                "USUARIO B PREFERENCIAS",
                "usuario-b-preferencias@example.com",
                "senha-usuario-b-preferencias",
                "USER",
                true
        );
        persistirPreferencia(usuarioA, "filters", "usuarios", "{\"origem\":\"A\"}");
        persistirPreferencia(usuarioB, "filters", "usuarios", "{\"origem\":\"B\"}");

        executarRemocao("filters", "usuarios")
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_TYPE));

        assertThat(preferenciaUsuarioRepository
                .findByUsuario_IdUsuarioAndContextoAndChave(usuarioA.getIdUsuario(), "filters", "usuarios"))
                .isEmpty();
        assertThat(preferenciaUsuarioRepository
                .findByUsuario_IdUsuarioAndContextoAndChave(usuarioB.getIdUsuario(), "filters", "usuarios"))
                .isPresent();

        executarRemocao("filters", "usuarios")
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_TYPE));

        assertThat(preferenciaUsuarioRepository
                .findByUsuario_IdUsuarioAndContextoAndChave(usuarioB.getIdUsuario(), "filters", "usuarios"))
                .isPresent();
    }

    @Test
    void parametrosAusentesOuInvalidosDevemRetornarBadRequest() throws Exception {
        mockMvc.perform(delete("/api/v1/preferencias/me/item")
                        .servletPath("/api/v1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .param("chave", "usuarios"))
                .andExpect(status().isBadRequest());

        executarRemocao(" ", "usuarios")
                .andExpect(status().isBadRequest());

        executarRemocao("x".repeat(121), "usuarios")
                .andExpect(status().isBadRequest());
    }

    @Test
    void rotaDeveExigirAutenticacaoBearer() throws Exception {
        mockMvc.perform(delete("/api/v1/preferencias/me/item")
                        .servletPath("/api/v1")
                        .param("contexto", "filters")
                        .param("chave", "usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
    }

    private org.springframework.test.web.servlet.ResultActions executarRemocao(String pContexto, String pChave) throws Exception {
        return mockMvc.perform(delete("/api/v1/preferencias/me/item")
                .servletPath("/api/v1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .param("contexto", pContexto)
                .param("chave", pChave));
    }

    private void persistirPreferencia(CUsuario pUsuario, String pContexto, String pChave, String pValorJson) {
        CPreferenciaUsuario preferencia = new CPreferenciaUsuario();
        preferencia.setUsuario(pUsuario);
        preferencia.setContexto(pContexto);
        preferencia.setChave(pChave);
        preferencia.setValorJson(pValorJson);
        preferenciaUsuarioRepository.saveAndFlush(preferencia);
    }
}
