package com.java.boilerplate.repository;

import com.java.boilerplate.model.CUsuarioAtivacao;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IUsuarioAtivacaoRepository extends IBaseRepository<CUsuarioAtivacao> {
    Optional<CUsuarioAtivacao> findByUsuario_IdUsuario(Long pIdUsuario);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from CUsuarioAtivacao a join fetch a.usuario where a.usuario.idUsuario = :pIdUsuario")
    Optional<CUsuarioAtivacao> findByUsuarioIdForUpdate(@Param("pIdUsuario") Long pIdUsuario);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from CUsuarioAtivacao a join fetch a.usuario where a.tokenHash = :pTokenHash")
    Optional<CUsuarioAtivacao> findByTokenHashForUpdate(@Param("pTokenHash") String pTokenHash);
}
