package com.java.boilerplate.repository;

import com.java.boilerplate.model.CUsuarioOtp;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface IUsuarioOtpRepository extends IBaseRepository<CUsuarioOtp> {
    Optional<CUsuarioOtp> findByUsuario_IdUsuario(Long pIdUsuario);
}
