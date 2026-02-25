package com.java.boilerplate.service.helpers;

import com.java.boilerplate.dto.auth.DTOOtp;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.UserOtp;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.repository.IUserOtpRepository;
import com.java.boilerplate.service.UsersService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {
    private final IUserOtpRepository otpRepository;
    private final UsersService usersService;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String emailFrom;

    public OtpService(IUserOtpRepository otpRepository, UsersService usersService, JavaMailSender mailSender) {
        this.otpRepository = otpRepository;
        this.usersService = usersService;
        this.mailSender = mailSender;
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

    @Transactional
    public UserOtp saveOtp(UserOtp otp) {
        return otpRepository.save(otp);
    }

    @Transactional(readOnly = true)
    public UserOtp findOtpByIdUser(Long idUser) {
        return otpRepository.findByUser_IdUser(idUser)
                .orElseThrow(() ->
                        new ExceptionsSystem(
                                "OTP code not found",
                                HttpStatus.UNAUTHORIZED
                        ));
    }

    @Transactional
    public Users validateOtpCode(DTOOtp request) {
        Users user = usersService.findByUsernameOrEmail(request.email());
        UserOtp otp = this.findOtpByIdUser(user.getIdUser());

        if (otp.getOtpCode() == null || !otp.getOtpCode().equals(request.code())) {
            throw new ExceptionsSystem(
                    "Invalid OTP code",
                    HttpStatus.UNAUTHORIZED
            );
        }

        if (otp.getExpiryDate().isBefore(LocalDateTime.now()) || otp.getUsed().equals(Boolean.TRUE)) {
            throw new ExceptionsSystem(
                    "OTP code expired",
                    HttpStatus.UNAUTHORIZED
            );
        }

        otp.setUsed(true);
        this.saveOtp(otp);
        return user;
    }

    @Transactional
    public void generateOtpCode(Users user) {
        UserOtp otpEntry = otpRepository.findByUser_IdUser(user.getIdUser()).orElse(new UserOtp());
        String code = String.format("%06d", new Random().nextInt(1000000));

        otpEntry.setUser(user);
        otpEntry.setOtpCode(code);
        otpEntry.setExpiryDate(LocalDateTime.now().plusMinutes(10));
        otpEntry.setUsed(false);

        this.saveOtp(otpEntry);
        this.sendOtpEmail(user.getEmail(), code);
    }
}
