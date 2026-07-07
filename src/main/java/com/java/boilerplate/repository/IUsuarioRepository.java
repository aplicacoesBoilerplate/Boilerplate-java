package com.java.boilerplate.repository;

import com.java.boilerplate.model.CUsuario;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUsuarioRepository extends IBaseRepository<CUsuario> {
    Optional<CUsuario> findByEmailIgnoreCase(String pEmail);
    boolean existsByEmailIgnoreCase(String pEmail);
}
