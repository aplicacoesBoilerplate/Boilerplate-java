package com.java.boilerplate.repository;

import com.java.boilerplate.model.CRefreshToken;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface IRefreshTokenRepository extends IBaseRepository<CRefreshToken> {
    Optional<CRefreshToken> findByTokenHash(String pTokenHash);

    void deleteByTokenHash(String pTokenHash);
}
