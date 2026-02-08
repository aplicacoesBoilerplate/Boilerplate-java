package com.java.boilerplate.service;

import com.java.boilerplate.config.security.TokenService;
import com.java.boilerplate.dto.auth.DTOAuth;
import com.java.boilerplate.enums.UserRoles;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.repository.IUsersRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService implements UserDetailsService {
    private final AuthenticationManager manager;
    private final PasswordEncoder encoder;
    private final TokenService tokenService;
    private final IUsersRepository usersRepository;

    public AuthService(@Lazy AuthenticationManager manager, PasswordEncoder encoder, TokenService tokenService, IUsersRepository usersRepository) {
        this.manager = manager;
        this.encoder = encoder;
        this.tokenService = tokenService;
        this.usersRepository = usersRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user = usersRepository.findByUsernameOrEmail(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return user;
    }

    public String login(DTOAuth data) {
        if (data.password() == null || data.password().isBlank()) {
            throw new ExceptionsSystem(
                    "The password field is required",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (data.password().length() < 8 || data.password().length() > 20) {
            throw new ExceptionsSystem(
                    "The password field must be between 8 and 20 characters long",
                    HttpStatus.BAD_REQUEST
            );
        }

        var authToken = new UsernamePasswordAuthenticationToken(data.usernameOrEmail(), data.password());
        var authentication = manager.authenticate(authToken);
        Users user = (Users) authentication.getPrincipal();
        return tokenService.generateToken(user);
    }

    @Transactional
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

    @Transactional(readOnly = true)
    public Users getMe() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Users user) {
             return usersRepository.findById(user.getIdUser())
                 .orElseThrow(() -> new ExceptionsSystem("User not found", HttpStatus.NOT_FOUND));
        }

        throw new ExceptionsSystem("User not authenticated", HttpStatus.UNAUTHORIZED);
    }
}
