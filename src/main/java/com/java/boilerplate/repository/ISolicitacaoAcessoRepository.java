package com.java.boilerplate.repository;

import com.java.boilerplate.enums.EStatusSolicitacaoAcesso;
import com.java.boilerplate.model.CSolicitacaoAcesso;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ISolicitacaoAcessoRepository extends IBaseRepository<CSolicitacaoAcesso> {
    Optional<CSolicitacaoAcesso> findByUsuario_EmailIgnoreCaseAndStatus(String pEmail, EStatusSolicitacaoAcesso pStatus);
    Optional<CSolicitacaoAcesso> findByUsuario_EmailIgnoreCase(String pEmail);
    Optional<CSolicitacaoAcesso> findByUsuario_IdUsuario(Long pIdUsuario);
}
