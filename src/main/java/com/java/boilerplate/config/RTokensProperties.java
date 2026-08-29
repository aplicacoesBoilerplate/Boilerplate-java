package com.java.boilerplate.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @description Record dos atributos do bloco 'tokens' no application.yml.
 */
@Validated
@ConfigurationProperties(prefix = "tokens")
public record RTokensProperties(
        @NotBlank(message = "JWT_SECRET deve ser informado")
        @Pattern(
                regexp = "^(?!boilerplate-java-dev-secret-change-me$).{32,}$",
                message = "JWT_SECRET deve possuir ao menos 32 caracteres e não pode usar o valor público do projeto"
        )
        String secret,
        @NotBlank(message = "JWT_ISSUER deve ser informado")
        String issuer,
        @Min(value = 5, message = "A validade do token deve ser de ao menos 5 minutos")
        @Max(value = 480, message = "A validade do token não pode exceder 8 horas")
        Long accessTokenMinutes,
        String googleClientId
) { }
