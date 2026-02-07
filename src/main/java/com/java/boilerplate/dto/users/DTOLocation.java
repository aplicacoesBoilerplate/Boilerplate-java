package com.java.boilerplate.dto.users;

import jakarta.validation.constraints.NotNull;

public record DTOLocation(
        @NotNull(message = "The latitude field is required")
        double latitude,

        @NotNull(message = "The longitude field is required")
        double longitude
) {}
