package com.java.boilerplate.config.security;

import com.java.boilerplate.dto.auth.RContextoSessaoBff;

import java.io.Serializable;
import java.security.Principal;

/**
 * @description Principal autenticado exclusivamente a partir do contexto de sessão revalidado pela Software Center.
 */
public record RPrincipalSessaoBff(RContextoSessaoBff contexto) implements Principal, Serializable {
    @Override
    public String getName() {
        return contexto.userId() == null ? "bff" : contexto.userId().toString();
    }
}
