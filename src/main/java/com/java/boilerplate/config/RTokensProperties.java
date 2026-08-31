package com.java.boilerplate.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Base64;

/**
 * @description Record dos atributos do bloco 'tokens' no application.yml.
 */
@Validated
@ConfigurationProperties(prefix = "tokens")
public record RTokensProperties(
        @NotBlank(message = "TOKEN_ENCRYPTION_KEY deve ser informado")
        String encryptionKey,
        @NotBlank(message = "JWT_ISSUER deve ser informado")
        String issuer,
        @Min(value = 5, message = "A validade do token deve ser de ao menos 5 minutos")
        @Max(value = 480, message = "A validade do token não pode exceder 8 horas")
        Long accessTokenMinutes,
        String googleClientId
) {
    /**
     * A256GCM exige uma chave AES de exatamente 256 bits, recebida em Base64
     * para não aceitar senhas ou valores textuais como material criptográfico.
     */
    @AssertTrue(message = "TOKEN_ENCRYPTION_KEY deve ser Base64 de uma chave aleatória com 32 bytes")
    public boolean isEncryptionKeyValida() {
        if (encryptionKey == null || encryptionKey.isBlank()) {
            return false;
        }

        try {
            return Base64.getDecoder().decode(encryptionKey).length == 32;
        } catch (IllegalArgumentException pException) {
            return false;
        }
    }
}
