package com.java.boilerplate.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

public record DTOError(
        String errorMessage,
        Map<String, Object> trace,

        @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss", timezone = "America/Sao_Paulo")
        LocalDateTime errorDateTime,

        HttpStatus errorStatusCode
) {}
