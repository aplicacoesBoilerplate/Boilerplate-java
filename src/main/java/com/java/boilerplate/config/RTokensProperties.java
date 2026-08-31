package com.java.boilerplate.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * @description Record dos atributos do bloco 'tokens' no application.yml.
 */
@Validated
@ConfigurationProperties(prefix = "tokens")
public record RTokensProperties(
        String encryptionKey,
        String legacySecret,
        @NotBlank(message = "JWT_ISSUER deve ser informado")
        String issuer,
        @Min(value = 5, message = "A validade do token deve ser de ao menos 5 minutos")
        @Max(value = 480, message = "A validade do token não pode exceder 8 horas")
        Long accessTokenMinutes,
        String googleClientId
) {
    /**
     * A256GCM exige uma chave AES de exatamente 256 bits. JWT_SECRET existe
     * somente para migrar instalações já publicadas para TOKEN_ENCRYPTION_KEY.
     */
    @AssertTrue(message = "TOKEN_ENCRYPTION_KEY deve ser Base64 de uma chave aleatória com 32 bytes ou JWT_SECRET legado deve possuir ao menos 32 caracteres")
    public boolean isEncryptionKeyValida() {
        return hasEncryptionKey() || hasLegacySecret();
    }

    public byte[] encryptionKeyBytes() {
        if (hasEncryptionKey()) {
            return Base64.getDecoder().decode(encryptionKey);
        }

        try {
            return MessageDigest.getInstance("SHA-256").digest(legacySecret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException pException) {
            throw new IllegalStateException("SHA-256 indisponível", pException);
        }
    }

    private boolean hasEncryptionKey() {
        if (encryptionKey == null || encryptionKey.isBlank()) {
            return false;
        }

        try {
            return Base64.getDecoder().decode(encryptionKey).length == 32;
        } catch (IllegalArgumentException pException) {
            return false;
        }
    }

    private boolean hasLegacySecret() {
        return legacySecret != null
                && legacySecret.length() >= 32
                && !"boilerplate-java-dev-secret-change-me".equals(legacySecret);
    }
}
