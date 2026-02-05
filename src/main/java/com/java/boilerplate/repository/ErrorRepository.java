package com.java.boilerplate.repository;

import com.java.boilerplate.model.ErrorsPersistidos;
import org.springframework.stereotype.Repository;

@Repository
public interface ErrorRepository {
    void persistirError(ErrorsPersistidos error);
}
