package com.java.boilerplate.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "ativacao")
public record RAtivacaoProperties(
        @NotBlank(message = "ATIVACAO_PEPPER deve ser informado")
        @Size(min = 32, message = "ATIVACAO_PEPPER deve possuir ao menos 32 caracteres")
        String pepper,
        @Min(value = 1, message = "A validade da ativação deve ser positiva")
        @Max(value = 1440, message = "A validade da ativação não pode exceder 1440 minutos")
        Integer expirationMinutes
) {
}
