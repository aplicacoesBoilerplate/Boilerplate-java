package com.java.boilerplate.repository;

import com.java.boilerplate.model.CRefreshToken;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IRefreshTokenRepository extends IBaseRepository<CRefreshToken> {
    Optional<CRefreshToken> findByTokenHash(String pTokenHash);
    void deleteByTokenHash(String pTokenHash);
}
