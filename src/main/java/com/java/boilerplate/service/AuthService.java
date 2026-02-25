package com.java.boilerplate.service;

import com.java.boilerplate.config.security.TokenService;
import com.java.boilerplate.dto.auth.DTOAuth;
import com.java.boilerplate.dto.auth.DTOOtp;
import com.java.boilerplate.enums.SubscriptionStatus;
import com.java.boilerplate.enums.UserRoles;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.UserOtp;
import com.java.boilerplate.model.UserSubscription;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.repository.IUserOtpRepository;
import com.java.boilerplate.repository.IUsersRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
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
import java.util.Random;

@Service
public class AuthService implements UserDetailsService {
    private final AuthenticationManager manager;
    private final PasswordEncoder encoder;
    private final TokenService tokenService;
    private final IUsersRepository usersRepository;
    private final UserSubscriptionService userSubscriptionService;
    private final JavaMailSender mailSender;
    private final IUserOtpRepository otpRepository;

    @Value("${spring.mail.username}")
    private String emailFrom;

    public AuthService(@Lazy AuthenticationManager manager, PasswordEncoder encoder, TokenService tokenService, IUsersRepository usersRepository, UserSubscriptionService userSubscriptionService, JavaMailSender mailSender, IUserOtpRepository otpRepository) {
        this.manager = manager;
        this.encoder = encoder;
        this.tokenService = tokenService;
        this.usersRepository = usersRepository;
        this.userSubscriptionService = userSubscriptionService;
        this.mailSender = mailSender;
        this.otpRepository = otpRepository;
    }

    private Users findUserById(Long idUser) {
        return usersRepository.findById(idUser)
                .orElseThrow(() -> new ExceptionsSystem(
                        "User not found",
                        HttpStatus.NOT_FOUND
                        )
                );

    }

    private Users findByUsernameOrEmail(String email) {
        Users user = usersRepository.findByUsernameOrEmail(email);
        if (user == null) {
            throw new ExceptionsSystem(
                    "User not found",
                    HttpStatus.NOT_FOUND
            );
        }
        return user;
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

    private void sendOtpEmail(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(emailFrom);
        message.setTo(to);
        message.setSubject("TZ Enconstros - Verify code");
        message.setText(
            String.format("""
                Use the code below to complete the verification process.
                
                Your verification code is: %s
                -> This code expires in 10 minutes
                """, code
            )
        );
        mailSender.send(message);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user = usersRepository.findByUsernameOrEmail(username);
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
        this.validateSubscription(user.getIdUser());
        return user;
    }

    @Transactional(readOnly = true)
    public Users getMeIgnoringSubscription() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Users principalUser) {
            Users freshUser = this.findUserById(principalUser.getIdUser());

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
    public void generateOtpCode(String email) {
        Users user = this.findByUsernameOrEmail(email);

        UserOtp otpEntry = otpRepository.findById(user.getIdUser()).orElse(new UserOtp());
        String code = String.format("%06d", new Random().nextInt(1000000));

        otpEntry.setUser(user);
        otpEntry.setOtpCode(code);
        otpEntry.setExpiryDate(LocalDateTime.now().plusMinutes(10));

        otpRepository.save(otpEntry);
        sendOtpEmail(user.getEmail(), code);
    }

    private Users validateOtpCode(DTOOtp request) {
        Users user = this.findByUsernameOrEmail(request.email());

        UserOtp otp = otpRepository.findByUser_IdUser(user.getIdUser())
                .orElseThrow(() ->
                        new ExceptionsSystem(
                                "OTP code not found",
                                HttpStatus.UNAUTHORIZED
                        ));

        if (otp.getOtpCode() == null || !otp.getOtpCode().equals(request.code())) {
            throw new ExceptionsSystem(
                    "Invalid OTP code",
                    HttpStatus.UNAUTHORIZED
            );
        }

        if (otp.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new ExceptionsSystem(
                    "OTP code expired",
                    HttpStatus.UNAUTHORIZED
            );
        }

        return user;
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
        newUser.setIsOnline(true);
        newUser.setIsActive(false);
        Users savedUser = usersRepository.save(newUser);

        UserSubscription subscription = new UserSubscription();
        subscription.setUser(savedUser);
        subscription.setExpireAt(LocalDateTime.now().plusDays(60));
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        userSubscriptionService.save(subscription);

        this.generateOtpCode(savedUser.getEmail());
        return savedUser;
    }

    @Transactional
    public Users verifyAccount(DTOOtp request) {
        Users userInDB = this.findByUsernameOrEmail(request.email());
        this.validateOtpCode(request);
        userInDB.setIsActive(true);
        usersRepository.save(userInDB);
        return userInDB;
    }

    @Transactional
    public String resetPasswordWithOtp(DTOOtp request) {
        Users user = this.validateOtpCode(request);
        user.setPassword(encoder.encode(request.password()));
        user.setIsOnline(true);
        usersRepository.save(user);
        return this.login(new DTOAuth(request.email(), request.password()));
    }
}
