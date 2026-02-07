package com.java.boilerplate.dto.users;

import jakarta.validation.constraints.NotNull;

public record DTORequestWithinRadius(
        @NotNull(message = "The location field is required")
        DTOLocation location,

        @NotNull(message = "The radius field is required")
        Long radius
) { }
