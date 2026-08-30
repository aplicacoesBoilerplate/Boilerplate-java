package com.java.boilerplate.config.security;

import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.service.CRbacService;
import java.util.function.Supplier;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

@Component
public class CAutorizacaoRbacManager implements AuthorizationManager<RequestAuthorizationContext> {
    private final CRbacService rbacService;

    public CAutorizacaoRbacManager(CRbacService pRbacService) {
        this.rbacService = pRbacService;
    }

    @Override
    public AuthorizationDecision authorize(
            Supplier<? extends Authentication> pAuthentication, RequestAuthorizationContext pContext) {
        Authentication authentication = pAuthentication.get();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }

        if (authentication.getPrincipal() instanceof CUsuario usuario) {
            return new AuthorizationDecision(rbacService.usuarioPodeAcessarEndpoint(
                    usuario,
                    pContext.getRequest().getMethod(),
                    pContext.getRequest().getServletPath()
            ));
        }

        return new AuthorizationDecision(false);
    }

}
