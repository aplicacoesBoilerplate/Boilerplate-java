package com.java.boilerplate.repository;

import com.java.boilerplate.model.CUsuario;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUsuarioRepository extends IBaseRepository<CUsuario> {
    Optional<CUsuario> findByEmailIgnoreCase(String pEmail);
    Optional<CUsuario> findByGoogleSubject(String pGoogleSubject);
    boolean existsByEmailIgnoreCase(String pEmail);

    @Query("""
            select case when count(f) > 0 then true else false end
            from CUsuario u
            join u.cargo c
            join c.funcionalidades f
            where u.idUsuario = :pIdUsuario
              and f.funcionalidade = :pFuncionalidade
              and f.liberado = true
            """)
    boolean possuiFuncionalidadeLiberada(
            @Param("pIdUsuario") Long pIdUsuario,
            @Param("pFuncionalidade") String pFuncionalidade);
}
