package com.java.boilerplate.repository;

import com.java.boilerplate.model.CUsuario;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface IUsuarioRepository extends IBaseRepository<CUsuario> {
    Optional<CUsuario> findByEmailIgnoreCase(String pEmail);

    boolean existsByEmailIgnoreCase(String pEmail);
}
