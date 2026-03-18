package com.java.boilerplate.service.helpers;

import com.java.boilerplate.dto.DTOParamsSendingEmail;
import com.java.boilerplate.dto.auth.DTOOtp;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.UserOtp;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.repository.IUserOtpRepository;
import com.java.boilerplate.service.UsersService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;

@Service
public class OtpService {
    private final IUserOtpRepository otpRepository;
    private final UsersService usersService;
    private final SendEmailService emailService;

    public OtpService(IUserOtpRepository otpRepository, UsersService usersService, SendEmailService emailService) {
        this.otpRepository = otpRepository;
        this.usersService = usersService;
        this.emailService = emailService;
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
                                "O código informado não foi encontrado para o usuário atual, código inválido",
                                HttpStatus.UNAUTHORIZED
                        ));
    }

    @Transactional
    public Users validateOtpCode(DTOOtp request) {
        Users user = usersService.findByUsernameOrEmail(request.email());
        UserOtp otp = this.findOtpByIdUser(user.getIdUser());

        if (otp.getOtpCode() == null || !otp.getOtpCode().equals(request.code())) {
            throw new ExceptionsSystem(
                    "Código de verificação inválido",
                    HttpStatus.UNAUTHORIZED
            );
        }

        if (otp.getExpiryDate().isBefore(LocalDateTime.now()) || otp.getUsed().equals(Boolean.TRUE)) {
            throw new ExceptionsSystem(
                    "Código de verificação expirado",
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

        DTOParamsSendingEmail emailRequest = new DTOParamsSendingEmail(
                user.getEmail(),
                "TZ Encontros - Verifique a sua conta",
                "otp-email",
                Map.of("username", user.getUserUsername(), "code", code)
        );

        emailService.sendEmail(emailRequest);
    }
}
