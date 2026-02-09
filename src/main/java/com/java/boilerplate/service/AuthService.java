package com.java.boilerplate.service;

import com.java.boilerplate.config.security.TokenService;
import com.java.boilerplate.dto.auth.DTOAuth;
import com.java.boilerplate.enums.SubscriptionStatus;
import com.java.boilerplate.enums.UserRoles;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.UserSubscription;
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

import java.time.LocalDateTime;

@Service
public class AuthService implements UserDetailsService {
    private final AuthenticationManager manager;
    private final PasswordEncoder encoder;
    private final TokenService tokenService;
    private final IUsersRepository usersRepository;
    private final UserSubscriptionService userSubscriptionService;

    public AuthService(@Lazy AuthenticationManager manager, PasswordEncoder encoder, TokenService tokenService, IUsersRepository usersRepository, UserSubscriptionService userSubscriptionService) {
        this.manager = manager;
        this.encoder = encoder;
        this.tokenService = tokenService;
        this.usersRepository = usersRepository;
        this.userSubscriptionService = userSubscriptionService;
    }

    private void validateSubscription(Long userId) {
        UserSubscription subscription = userSubscriptionService.findByUser_IdUser(userId);

        if (!subscription.isValid()) {
            if (subscription.getStatus() == SubscriptionStatus.ACTIVE) {
                subscription.setStatus(SubscriptionStatus.OVERDUE);
                userSubscriptionService.save(subscription);
            }

            throw new ExceptionsSystem(
                    "Subscription expired. Please renew your plan.",
                    HttpStatus.PAYMENT_REQUIRED
            );
        }
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
        var authToken = new UsernamePasswordAuthenticationToken(data.usernameOrEmail(), data.password());
        var authentication = manager.authenticate(authToken);
        Users user = (Users) authentication.getPrincipal();
        if (user == null) {
            throw new ExceptionsSystem(
                    "Error logging in, user not found!",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        this.validateSubscription(user.getIdUser());
        return tokenService.generateToken(user);
    }

    @Transactional
    public Users getMe() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Users user) {
            this.validateSubscription(user.getIdUser());
             return usersRepository.findById(user.getIdUser())
                 .orElseThrow(() -> new ExceptionsSystem("User not found", HttpStatus.NOT_FOUND));
        }

        throw new ExceptionsSystem("User not authenticated", HttpStatus.UNAUTHORIZED);
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
        Users savedUser = usersRepository.save(newUser);

        UserSubscription subscription = new UserSubscription();
        subscription.setUser(savedUser);
        subscription.setExpireAt(LocalDateTime.now().plusDays(60));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        userSubscriptionService.save(subscription);

        return savedUser;
    }
}
