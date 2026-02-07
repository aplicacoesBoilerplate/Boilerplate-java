package com.java.boilerplate.service;

import com.java.boilerplate.config.security.TokenService;
import com.java.boilerplate.dto.auth.DTOAuth;
import com.java.boilerplate.enums.UserRoles;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.repository.IUsersRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager manager;
    private final PasswordEncoder encoder;
    private final TokenService tokenService;
    private final IUsersRepository usersRepository;

    public AuthService(AuthenticationManager manager, PasswordEncoder encoder, TokenService tokenService, IUsersRepository usersRepository) {
        this.manager = manager;
        this.encoder = encoder;
        this.tokenService = tokenService;
        this.usersRepository = usersRepository;
    }

    public String login(DTOAuth data) {
        var authToken = new UsernamePasswordAuthenticationToken(data.usernameOrEmail(), data.password());
        var authentication = manager.authenticate(authToken);
        Users user = (Users) authentication.getPrincipal();
        return tokenService.generateToken(user);
    }

    public Users register(Users newUser) {
        Users userEmail = usersRepository.findByUsernameOrEmail(newUser.getEmail());
        Users userUsername = usersRepository.findByUsernameOrEmail(newUser.getUserUsername());

        if (userEmail != null || userUsername != null) {
            throw new ExceptionsSystem(
                    "An account with this login information was found. Please log in to access it or recover your password!",
                    HttpStatus.CONFLICT
            );
        }

        String passwordEncode = encoder.encode(newUser.getPassword());
        newUser.setPassword(passwordEncode);
        newUser.setRole(UserRoles.USER);
        return usersRepository.save(newUser);
    }

    public Users getMe(String token) {
        var subject = tokenService.validateToken(token);
        Users user = usersRepository.findByUsernameOrEmail(subject);
        if (user == null) throw new ExceptionsSystem(
                "User not found",
                HttpStatus.NOT_FOUND
        );
        return user;
    }
}
