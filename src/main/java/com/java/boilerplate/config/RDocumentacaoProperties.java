package com.java.boilerplate.config;

import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @description Record dos atributos do bloco 'documentação' no application.yml.
 */
@Validated
@ConfigurationProperties(prefix = "documentacao")
public record RDocumentacaoProperties(
        boolean enabled,
        String usuario,
        String senha,
        String senhaHash
) {
    @AssertTrue(message = "A documentação exige usuário não público e senha forte ou hash BCrypt")
    public boolean isCredenciaisSeguras() {
        if (!enabled) {
            return true;
        }

        if (usuario == null || usuario.isBlank()
                || "DeveloperArea".equals(usuario)
                || "documentacao-local".equals(usuario)) {
            return false;
        }

        boolean senhaValida = senha != null
                && senha.length() >= 12
                && !"boilerplate".equals(senha)
                && !"troque-esta-senha".equals(senha);
        boolean hashValido = senhaHash != null && senhaHash.matches("^(\\{bcrypt})?\\$2[aby]\\$\\d{2}\\$.{53}$");
        return senhaValida || hashValido;
    }
}
