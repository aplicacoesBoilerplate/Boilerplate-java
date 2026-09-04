package com.java.boilerplate.service;

import com.java.boilerplate.config.security.CTokenService;
import com.java.boilerplate.dto.usuarios.RUsuario;
import com.java.boilerplate.model.CCargoRbac;
import com.java.boilerplate.model.CFuncionalidadeCargoRbac;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.ICargoRbacRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class CUsuarioAuthorizationSecurityTests {
    @Autowired
    private CUsuarioService usuarioService;
    @Autowired
    private CTokenService tokenService;
    @Autowired
    private ICargoRbacRepository cargoRepository;
    private Long idAdministrador;
    private CUsuario gestorGlobal;

    @BeforeEach
    void criarDadosDoCenario() {
        CCargoRbac cargoAdmin = cargoRepository.findByPapel("ADMIN").orElseThrow();
        CFuncionalidadeCargoRbac funcionalidade = new CFuncionalidadeCargoRbac();
        funcionalidade.setFuncionalidade("gerenciarRegistros");
        funcionalidade.setLiberado(true);
        cargoAdmin.definirFuncionalidades(List.of(funcionalidade));
        cargoRepository.saveAndFlush(cargoAdmin);
        idAdministrador = usuarioService.criarUsuarioSistema(
                "Administrador do teste",
                "admin-test@example.com",
                "admin-test-password-secure",
                "USER",
                true
        ).getIdUsuario();
        gestorGlobal = usuarioService.criarUsuarioSistema(
                "Gestor do teste",
                "gestor-test@example.com",
                "gestor-test-password-secure",
                "ADMIN",
                true
        );
    }

    @Test
    @WithMockUser(roles = "USER")
    void usuarioComumPodeLerUsuarioQuandoRequisicaoJaFoiAutorizadaPeloRbac() {
        assertThat(usuarioService.buscarPorId(idAdministrador).email()).isEqualTo("admin-test@example.com");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administradorDeveManterAcessoAoDiretorio() {
        assertThat(usuarioService.buscarPorId(idAdministrador).email()).isEqualTo("admin-test@example.com");
    }

    @Test
    void reativarUsuarioNaoDeveRestaurarTokenEmitidoAntesDaDesativacao() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        gestorGlobal, gestorGlobal.getPassword(), gestorGlobal.getAuthorities()));
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
