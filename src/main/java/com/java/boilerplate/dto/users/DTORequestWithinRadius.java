package com.java.boilerplate.dto.users;

import jakarta.validation.constraints.NotNull;

public record DTORequestWithinRadius(
        @NotNull(message = "A informação da localização é obrigatória")
        DTOLocation location,

        @NotNull(message = "A informação do raio de busca é obrigatória")
        Long radius
) { }
