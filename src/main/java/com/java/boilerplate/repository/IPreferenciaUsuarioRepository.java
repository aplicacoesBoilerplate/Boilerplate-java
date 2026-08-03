package com.java.boilerplate.repository;

import com.java.boilerplate.model.CPreferenciaUsuario;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface IPreferenciaUsuarioRepository extends IBaseRepository<CPreferenciaUsuario> {
    List<CPreferenciaUsuario> findByUsuario_IdUsuarioOrderByContextoAscChaveAsc(Long pIdUsuario);

    Optional<CPreferenciaUsuario> findByUsuario_IdUsuarioAndContextoAndChave(
            Long pIdUsuario, String pContexto, String pChave);
}
