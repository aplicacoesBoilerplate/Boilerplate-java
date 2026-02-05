package com.java.boilerplate.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public class ExceptionsSystem extends RuntimeException implements IErrors {
    private final Integer statusCode;
    private final LocalDateTime dateTime;

    public ExceptionsSystem(String message, HttpStatus statusCode) {
        super(message);
        this.statusCode = statusCode.value();
        this.dateTime = LocalDateTime.now();
    }

    @Override
    public String getErro() {
        return super.getMessage();
    }

    @Override
    public Integer getStatusCode() {
        return this.statusCode;
    }

    @Override
    public LocalDateTime getHoraErro() {
        return this.dateTime;
    }
}
