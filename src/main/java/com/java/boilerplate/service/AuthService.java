package com.java.boilerplate.service;

import com.java.boilerplate.config.security.TokenService;
import com.java.boilerplate.dto.auth.DTOAuth;
import com.java.boilerplate.dto.auth.DTOOtp;
import com.java.boilerplate.enums.SubscriptionStatus;
import com.java.boilerplate.enums.UserRoles;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.UserSubscription;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.service.helpers.OtpService;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.function.Function;

@Service
public class AuthService implements UserDetailsService {
    private final AuthenticationManager manager;
    private final PasswordEncoder encoder;
    private final TokenService tokenService;
    private final UsersService usersService;
    private final UserSubscriptionService userSubscriptionService;
    private final OtpService otpService;

    public AuthService(@Lazy AuthenticationManager manager, PasswordEncoder encoder, TokenService tokenService, @Lazy UsersService usersService, UserSubscriptionService userSubscriptionService, OtpService otpService) {
        this.manager = manager;
        this.encoder = encoder;
        this.tokenService = tokenService;
        this.usersService = usersService;
        this.userSubscriptionService = userSubscriptionService;
        this.otpService = otpService;
    }

    @Transactional
    public void generateOtpCode(String email) {
        Users user = usersService.findByUsernameOrEmail(email);
        otpService.generateOtpCode(user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user = usersService.findByUsernameOrEmail(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }
        return user;
    }

    @Transactional
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
        user.setIsOnline(true);
        return tokenService.generateToken(user);
    }

    @Transactional
    public Users getMe() {
        Users user = this.getMeIgnoringSubscription();
        userSubscriptionService.validateSubscription(user.getIdUser());
        return user;
    }

    @Transactional(readOnly = true)
    public Users getMeIgnoringSubscription() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Users principalUser) {
            Users freshUser = usersService.findById(principalUser.getIdUser());

            if (!freshUser.getIsActive()) {
                throw new DisabledException("User account is inactive");
            }

            return freshUser;
        }

        throw new ExceptionsSystem(
                "User not authenticated",
                HttpStatus.UNAUTHORIZED
        );
    }

    @Transactional
    public Users register(Users newUser) {
        newUser.setIsActive(false);
        newUser.setRole(UserRoles.USER);
        Users savedUser = usersService.saveUser(newUser);

        UserSubscription subscription = new UserSubscription();
        subscription.setUser(savedUser);
        subscription.setExpireAt(LocalDateTime.now().plusDays(60));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        userSubscriptionService.save(subscription);

        otpService.generateOtpCode(savedUser);
        return savedUser;
    }

    @Transactional
    public <T> T executeWithValidOtp(DTOOtp request, Function<Users, T> onSuccessAction) {
        Users user = otpService.validateOtpCode(request);
        return onSuccessAction.apply(user);
    }

    @Transactional
    public Users verifyAccount(DTOOtp request) {
        return executeWithValidOtp(request, (user) -> {
            user.setIsOnline(true);
            user.setIsActive(true);
            return usersService.saveEntity(user);
        });
    }

    @Transactional
    public String resetPasswordWithOtp(DTOOtp request) {
        return executeWithValidOtp(request, (user) -> {
            user.setPassword(encoder.encode(request.password()));
            user.setIsOnline(true);
            user.setIsActive(true);
            usersService.saveEntity(user);
            return this.login(new DTOAuth(request.email(), request.password()));
        });
    }
}
