package com.java.boilerplate.dto;

import java.time.LocalDateTime;

public record DTOErrorResponse(
        String arquivoError,
        String classError,
        String metodoError,
        int linhaError,
        LocalDateTime horaError
) {}
