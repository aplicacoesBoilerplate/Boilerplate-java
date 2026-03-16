package com.java.boilerplate.service;

import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.RefreshToken;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.repository.IRefreshTokenRepository;
import com.java.boilerplate.service.helpers.HashUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final IRefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(IRefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Transactional
    public String createRefreshToken(Users user) {
        String plainToken = UUID.randomUUID().toString();
        String hashToken = HashUtil.generateSha256(plainToken);

        RefreshToken refreshToken = refreshTokenRepository.findById(user.getIdUser())
                .orElse(new RefreshToken());

        refreshToken.setUser(user);
        refreshToken.setTokenHash(hashToken);
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(30));

        refreshTokenRepository.save(refreshToken);

        return plainToken;
    }

    @Transactional(readOnly = true)
    public void verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(token);
            throw new ExceptionsSystem("Refresh expired token. Log in again.", HttpStatus.UNAUTHORIZED);
        }
    }

    @Transactional(readOnly = true)
    public RefreshToken findByToken(String plainToken) {
        String hashToken = HashUtil.generateSha256(plainToken);
        return refreshTokenRepository.findByTokenHash(hashToken)
                .orElseThrow(() -> new ExceptionsSystem("Refresh token não encontrado ou inválido.", HttpStatus.UNAUTHORIZED));
    }

    @Transactional
    public void deleteByToken(String plainToken) {
        String hashToken = HashUtil.generateSha256(plainToken);
        refreshTokenRepository.deleteByTokenHash(hashToken);
    }
}
