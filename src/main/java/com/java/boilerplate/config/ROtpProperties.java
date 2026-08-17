package com.java.boilerplate.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "otp")
public record ROtpProperties(
        @NotBlank(message = "OTP_PEPPER deve ser informado")
        @Size(min = 32, message = "OTP_PEPPER deve possuir ao menos 32 caracteres")
        String pepper,
        @Min(value = 1, message = "A validade do OTP deve ser positiva")
        @Max(value = 30, message = "A validade do OTP não pode exceder 30 minutos")
        Integer expirationMinutes,
        @Min(value = 1, message = "O limite de tentativas do OTP deve ser positivo")
        @Max(value = 10, message = "O limite de tentativas do OTP não pode exceder 10")
        Integer maxAttempts
) {
}
