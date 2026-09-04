package com.java.boilerplate.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.java.boilerplate.dto.usuarios.RUsuario;
import com.java.boilerplate.service.CUsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@ActiveProfiles("test")
@SpringBootTest(properties = "app.expose-error-trace=true")
@AutoConfigureMockMvc
@Import(CPreAuthorizeErrorHandlingIntegrationTests.CNegacaoPreAuthorizeController.class)
class CPreAuthorizeErrorHandlingIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "USER")
    void cadastroNegadoPeloPreAuthorizeDeveRetornar403Sanitizado() throws Exception {
        mockMvc.perform(get("/api/v1/preferencias/teste-preauthorize").servletPath("/api/v1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Operação não autorizada"))
                .andExpect(jsonPath("$.codigo").doesNotExist())
                .andExpect(jsonPath("$.dados").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @RestController
    static class CNegacaoPreAuthorizeController {
        private final CUsuarioService usuarioService;

        CNegacaoPreAuthorizeController(CUsuarioService pUsuarioService) {
            this.usuarioService = pUsuarioService;
        }

        @GetMapping("/preferencias/teste-preauthorize")
        ResponseEntity<Void> cadastrarUsuario() {
            usuarioService.cadastrar(new RUsuario(
                    null,
                    "Usuário sem permissão",
                    "sem-permissao@example.com",
                    null,
                    null,
                    false,
                    true,
                    "USER",
                    null));
            return ResponseEntity.noContent().build();
        }
    }
}
