package com.java.boilerplate.repository;

import com.java.boilerplate.model.CUsuarioOtp;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IUsuarioOtpRepository extends IBaseRepository<CUsuarioOtp> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CUsuarioOtp> findByUsuario_IdUsuario(Long pIdUsuario);
}
