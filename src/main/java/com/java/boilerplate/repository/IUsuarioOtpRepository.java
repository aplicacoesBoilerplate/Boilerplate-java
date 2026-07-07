package com.java.boilerplate.repository;

import com.java.boilerplate.model.CUsuarioOtp;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUsuarioOtpRepository extends IBaseRepository<CUsuarioOtp> {
    Optional<CUsuarioOtp> findByUsuario_IdUsuario(Long pIdUsuario);
}
