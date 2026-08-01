package com.java.boilerplate.integration.softwarecenter;

import java.time.Instant;
import java.util.Set;

/**
 * @description Contratos privados do cliente BFF para a Integration API da Software Center.
 */
public final class RSoftwareCenterDtos {
    private RSoftwareCenterDtos() {
    }

    public record RCriarSessaoSenha(String email, String senha, String tenantSubdominio) {
    }

    public record RCriarSessaoGoogle(String credential, String tenantSubdominio) {
    }

    public record RContextoSessao(
            Long userId,
            Long tenantId,
            String tenantSubdomain,
            Long membershipId,
            Long applicationId,
            Long roleId,
            String roleName,
            String roleIcon,
            Set<String> permissions,
            Set<String> capabilities
    ) {
    }

    public record RSessaoCriada(String sessionId, Instant expiresAt, RContextoSessao context) {
    }

    public record RSessaoValidada(Instant expiresAt, RContextoSessao context) {
    }

    public record RCadastro(String nome, String email, String senha) {
    }

    public record RSolicitarRecuperacao(String email) {
    }

    public record RVerificarRecuperacao(String email, String otp) {
    }

    public record RRedefinirSenha(String email, String otp, String novaSenha) {
    }
}
