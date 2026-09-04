package com.java.boilerplate.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class CRbacManifestoHttpSecurityTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void manifestoDeveExigirAutenticacao() throws Exception {
        mockMvc.perform(get("/api/v1/rbac/manifesto").servletPath("/api/v1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void usuarioAutenticadoDeveReceberManifestoDosEndpointsRbacReais() throws Exception {
        mockMvc.perform(get("/api/v1/rbac/manifesto").servletPath("/api/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versaoContrato").value(1))
                .andExpect(jsonPath("$.recursos.*", hasSize(3)))
                .andExpect(jsonPath("$.recursos.Usuarios.acoes.consultar[*].metodo",
                        containsInAnyOrder("GET", "POST")))
                .andExpect(jsonPath("$.recursos.Usuarios.acoes.consultar[*].path",
                        containsInAnyOrder("/usuarios/**", "/usuarios/consulta")))
                .andExpect(jsonPath("$.recursos.Usuarios.acoes.gravar[0].path").value("/usuarios"))
                .andExpect(jsonPath("$.recursos.Rbac.acoes.consultar[*].path",
                        containsInAnyOrder("/rbac/cargos/**", "/rbac/cargos/consulta")))
                .andExpect(jsonPath("$.recursos.Erros.acoes.consultar[0].path").value("/erros/consulta"))
                .andExpect(jsonPath("$.recursos.Auth").doesNotExist())
                .andExpect(jsonPath("$.recursos.Preferencias").doesNotExist());
    }
}
