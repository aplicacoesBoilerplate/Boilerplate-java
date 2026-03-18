package com.java.boilerplate.dto.users;

import jakarta.validation.constraints.NotNull;

public record DTOLocation(
        @NotNull(message = "A informação da latitude é obrigatória")
        double latitude,

        @NotNull(message = "A informação da longitude é obrigatória")
        double longitude
) {}
