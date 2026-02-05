package com.java.boilerplate.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public interface IErrors {
    String getErro();
    Integer getStatusCode();
    LocalDateTime getHoraErro();
}
