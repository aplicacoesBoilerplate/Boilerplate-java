package com.java.boilerplate.service.helpers;

import com.java.boilerplate.config.ROtpProperties;
import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.model.CUsuarioOtp;
import com.java.boilerplate.repository.IUsuarioOtpRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;

@Service
public class COtpAttemptService {
    private static final String MENSAGEM_CODIGO_INVALIDO = "Código de recuperação inválido ou expirado";

    private final IUsuarioOtpRepository otpRepository;
    private final ROtpProperties otpProperties;

    public COtpAttemptService(IUsuarioOtpRepository pOtpRepository, ROtpProperties pOtpProperties) {
        this.otpRepository = pOtpRepository;
        this.otpProperties = pOtpProperties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = CExceptionsSystem.class)
    public void validar(Long pIdUsuario, String pHashRecebido, boolean pConsumir) {
        CUsuarioOtp otp = otpRepository.findByUsuario_IdUsuario(pIdUsuario)
                .orElseThrow(this::codigoInvalido);

        if (Boolean.TRUE.equals(otp.getUtilizado())
                || otp.getExpiraEm().isBefore(LocalDateTime.now())
                || otp.getTentativas() >= otpProperties.maxAttempts()) {
            throw codigoInvalido();
        }

        boolean corresponde = MessageDigest.isEqual(
                otp.getCodigo().getBytes(StandardCharsets.US_ASCII),
                pHashRecebido.getBytes(StandardCharsets.US_ASCII)
        );
        if (!corresponde) {
            int tentativas = otp.getTentativas() + 1;
            otp.setTentativas(tentativas);
            if (tentativas >= otpProperties.maxAttempts()) {
                otp.setUtilizado(true);
            }
            otpRepository.save(otp);
            throw codigoInvalido();
        }

        if (pConsumir) {
            otp.setUtilizado(true);
            otpRepository.save(otp);
        }
    }

    private CExceptionsSystem codigoInvalido() {
        return new CExceptionsSystem(MENSAGEM_CODIGO_INVALIDO, HttpStatus.UNAUTHORIZED);
    }
}
