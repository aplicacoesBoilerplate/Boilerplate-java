package com.java.boilerplate.exception;

import java.time.LocalDateTime;

public interface IErrors {
    String getErro();
    Integer getStatusCode();
    LocalDateTime getHoraErro();

}