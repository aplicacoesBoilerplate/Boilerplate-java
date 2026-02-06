package com.java.boilerplate.service;

import com.java.boilerplate.config.security.TokenService;
import com.java.boilerplate.dto.users.DTOUser;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.repository.IUsersRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final PasswordEncoder encoder;
    private final TokenService tokenService;
    private final IUsersRepository usersRepository;

    public AuthService(PasswordEncoder encoder, TokenService tokenService, IUsersRepository usersRepository) {
        this.encoder = encoder;
        this.tokenService = tokenService;
        this.usersRepository = usersRepository;
    }

    public Users register(Users newUser) {
        Users userEmail = (Users) usersRepository.findByUsernameOrEmail(newUser.getEmail());
        Users userUsername = (Users) usersRepository.findByUsernameOrEmail(newUser.getUsername());

        if (userEmail != null || userUsername != null) {
            throw new ExceptionsSystem(
                    "An account with this login information was found. Please log in to access it or recover your password!",
                    HttpStatus.CONFLICT
            );
        }

        String passwordEncode = encoder.encode(newUser.getPassword());
        newUser.setPassword(passwordEncode);
        return usersRepository.save(newUser);
    }

    public Users getMe(String token) {
        var subject = tokenService.validateToken(token);
        Users user = (Users) usersRepository.findByUsernameOrEmail(subject);
        if (user == null) throw new ExceptionsSystem(
                "User not found",
                HttpStatus.NOT_FOUND
        );
        return user;
    }
}
