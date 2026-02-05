package com.java.boilerplate.exception;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public interface IErrors {
    String getErro();
    Integer getStatusCode();
    LocalDateTime getHoraErro();

}