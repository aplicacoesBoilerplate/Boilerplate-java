package com.java.boilerplate.repository;

import com.java.boilerplate.model.CRefreshToken;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

import java.util.Optional;

@Repository
public interface IRefreshTokenRepository extends IBaseRepository<CRefreshToken> {
    Optional<CRefreshToken> findByTokenHash(String pTokenHash);
    @EntityGraph(attributePaths = {"usuario", "usuario.cargo"})
    @Query("select t from CRefreshToken t where t.tokenHash = :pHash and t.expiraEm > :pAgora")
    Optional<CRefreshToken> findActiveByHash(@Param("pHash") String pHash, @Param("pAgora") LocalDateTime pAgora);
    void deleteByTokenHash(String pTokenHash);
}
