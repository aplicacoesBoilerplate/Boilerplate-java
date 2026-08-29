package com.java.boilerplate.config.security;

import com.java.boilerplate.model.CCargoRbac;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.service.CRbacService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CAutorizacaoRbacManagerTests {
    @Test
    void deveAutorizarJwtLocalPelaPermissaoDoCargo() {
        CRbacService rbacService = mock(CRbacService.class);
        CAutorizacaoRbacManager manager = new CAutorizacaoRbacManager(rbacService);
        CUsuario usuario = criarUsuarioUser();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/usuarios/3");
        request.setServletPath("/usuarios/3");
        when(rbacService.usuarioPodeAcessarEndpoint(usuario, "GET", "/usuarios/3")).thenReturn(true);

        AuthorizationDecision decisao = manager.authorize(
                () -> new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()),
                new RequestAuthorizationContext(request)
        );

        assertThat(decisao.isGranted()).isTrue();
        verify(rbacService).usuarioPodeAcessarEndpoint(usuario, "GET", "/usuarios/3");
    }

    @Test
    void deveNegarJwtLocalSemPermissaoDoCargo() {
        CRbacService rbacService = mock(CRbacService.class);
        CAutorizacaoRbacManager manager = new CAutorizacaoRbacManager(rbacService);
        CUsuario usuario = criarUsuarioUser();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/usuarios/3");
        request.setServletPath("/usuarios/3");
        when(rbacService.usuarioPodeAcessarEndpoint(usuario, "GET", "/usuarios/3")).thenReturn(false);

        AuthorizationDecision decisao = manager.authorize(
                () -> new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()),
                new RequestAuthorizationContext(request)
        );

        assertThat(decisao.isGranted()).isFalse();
    }

    private CUsuario criarUsuarioUser() {
        CCargoRbac cargo = new CCargoRbac();
        cargo.setPapel("USER");
        cargo.setAtivo(true);
        CUsuario usuario = new CUsuario();
        usuario.setCargo(cargo);
        usuario.setAtivo(true);
        return usuario;
    }
}
