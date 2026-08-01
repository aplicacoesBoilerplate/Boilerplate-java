package com.java.boilerplate.dto.auth;

import java.io.Serializable;
import java.time.Instant;
import java.util.Set;

/**
 * @description Contexto sanitizado persistido no servidor e retornado à SPA, sem tokens ou identificadores opacos da SC.
 */
public record RContextoSessaoBff(
        Long userId,
        Long tenantId,
        String tenantSubdomain,
        Long membershipId,
        Long applicationId,
        Long roleId,
        String roleName,
        String roleIcon,
        Set<String> permissions,
        Set<String> capabilities,
        Instant expiresAt
) implements Serializable {
    public RContextoSessaoBff {
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
    }
}
