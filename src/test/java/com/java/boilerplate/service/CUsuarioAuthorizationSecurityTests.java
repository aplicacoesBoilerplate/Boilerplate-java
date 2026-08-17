package com.java.boilerplate.service;

import com.java.boilerplate.config.security.CTokenService;
import com.java.boilerplate.dto.usuarios.RUsuario;
import com.java.boilerplate.model.CUsuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
class CUsuarioAuthorizationSecurityTests {
    @Autowired
    private CUsuarioService usuarioService;
    @Autowired
    private CTokenService tokenService;
    private Long idAdministrador;

    @BeforeEach
    void criarDadosDoCenario() {
        idAdministrador = usuarioService.criarUsuarioSistema(
                "Administrador do teste",
                "admin-test@example.com",
                "admin-test-password-secure",
                "ADMIN",
                true
        ).getIdUsuario();
    }

    @Test
    @WithMockUser(roles = "USER")
    void usuarioComumNaoDeveLerDiretorioAdministrativoNemPorChamadaDiretaAoService() {
        assertThatThrownBy(() -> usuarioService.buscarPorId(idAdministrador))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administradorDeveManterAcessoAoDiretorio() {
        assertThat(usuarioService.buscarPorId(idAdministrador).email()).isEqualTo("admin-test@example.com");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void reativarUsuarioNaoDeveRestaurarTokenEmitidoAntesDaDesativacao() {
        CUsuario usuario = usuarioService.buscarEntidadePorId(idAdministrador);
        String token = tokenService.gerarToken(usuario);
        RUsuario atual = usuarioService.buscarPorId(idAdministrador);

        usuarioService.atualizar(idAdministrador, new RUsuario(
                atual.id(), atual.nome(), atual.email(), atual.avatar(), atual.telefone(), atual.notificar(), false, atual.papel(), atual.auditoria()
        ));
        usuarioService.atualizar(idAdministrador, new RUsuario(
                atual.id(), atual.nome(), atual.email(), atual.avatar(), atual.telefone(), atual.notificar(), true, atual.papel(), atual.auditoria()
        ));

        assertThat(tokenService.validarToken(token)).isNull();
    }
}
