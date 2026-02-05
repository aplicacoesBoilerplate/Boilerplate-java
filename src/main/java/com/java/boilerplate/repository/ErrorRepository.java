package com.java.boilerplate.repository;

import com.java.boilerplate.model.ErrorsPersistidos;

public interface ErrorRepository {
    void persistirError(ErrorsPersistidos error);
}
