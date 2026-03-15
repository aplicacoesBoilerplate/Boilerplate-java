package com.java.boilerplate.repository;

import com.java.boilerplate.model.RefreshToken;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IRefreshTokenRepository extends IBaseRepository<RefreshToken> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void deleteByUser_IdUser(Long idUser);
    void deleteByTokenHash(String tokenHash);
}
