package com.java.boilerplate.config.security;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;

/**
 * @description Aceita o token bruto do cookie no header da SPA e mantém proteção XOR para formulários server-side.
 */
public class CSpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
    public static final String ATRIBUTO_TOKEN_BRUTO = CSpaCsrfTokenRequestHandler.class.getName() + ".TOKEN_BRUTO";

    private final CsrfTokenRequestHandler plainHandler = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xorHandler = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest pRequest, HttpServletResponse pResponse, Supplier<CsrfToken> pCsrfToken) {
        xorHandler.handle(pRequest, pResponse, pCsrfToken);
        CsrfToken tokenBruto = pCsrfToken.get();
        pRequest.setAttribute(ATRIBUTO_TOKEN_BRUTO, tokenBruto);
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest pRequest, CsrfToken pCsrfToken) {
        String header = pRequest.getHeader(pCsrfToken.getHeaderName());
        return (StringUtils.hasText(header) ? plainHandler : xorHandler).resolveCsrfTokenValue(pRequest, pCsrfToken);
    }
}
