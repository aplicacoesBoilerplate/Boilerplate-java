package com.java.boilerplate.config.security;

import java.util.List;
import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

@Component
public class CAutorizacaoRbacManager implements AuthorizationManager<RequestAuthorizationContext> {
    private static final List<RRegraPermissao> REGRAS_PERMISSAO = List.of(
            new RRegraPermissao("GET", "/usuarios/consulta", "boilerplate.usuarios.read"),
            new RRegraPermissao("POST", "/usuarios/consulta", "boilerplate.usuarios.read"),
            new RRegraPermissao("POST", "/usuarios/search", "boilerplate.usuarios.read"),
            new RRegraPermissao("GET", "/usuarios/*", "boilerplate.usuarios.read"),
            new RRegraPermissao("POST", "/usuarios", "boilerplate.usuarios.write"),
            new RRegraPermissao("PUT", "/usuarios/*", "boilerplate.usuarios.write"),
            new RRegraPermissao("DELETE", "/usuarios/*", "boilerplate.usuarios.write"),
            new RRegraPermissao("GET", "/rbac/cargos", "boilerplate.rbac.read"),
            new RRegraPermissao("POST", "/rbac/cargos/consulta", "boilerplate.rbac.read"),
            new RRegraPermissao("GET", "/rbac/cargos/*", "boilerplate.rbac.read"),
            new RRegraPermissao("POST", "/rbac/cargos", "boilerplate.rbac.manage"),
            new RRegraPermissao("PUT", "/rbac/cargos/*", "boilerplate.rbac.manage"),
            new RRegraPermissao("DELETE", "/rbac/cargos/*", "boilerplate.rbac.manage"),
            new RRegraPermissao("GET", "/preferencias/me", "boilerplate.preferencias.read"),
            new RRegraPermissao("PUT", "/preferencias/**", "boilerplate.preferencias.write"),
            new RRegraPermissao("POST", "/erros/consulta", "boilerplate.erros.read"));

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public AuthorizationDecision authorize(
            Supplier<? extends Authentication> pAuthentication, RequestAuthorizationContext pContext) {
        Authentication authentication = pAuthentication.get();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof RPrincipalSessaoBff principal)) {
            return new AuthorizationDecision(false);
        }

        String permissao = resolverPermissao(
                pContext.getRequest().getMethod(), pContext.getRequest().getServletPath());
        return new AuthorizationDecision(
                permissao != null && principal.contexto().permissions().contains(permissao));
    }

    private String resolverPermissao(String pMetodo, String pCaminho) {
        return REGRAS_PERMISSAO.stream()
                .filter(pRegra ->
                        pRegra.metodo().equalsIgnoreCase(pMetodo) && pathMatcher.match(pRegra.caminho(), pCaminho))
                .map(RRegraPermissao::permissao)
                .findFirst()
                .orElse(null);
    }

    private record RRegraPermissao(String metodo, String caminho, String permissao) {}
}
