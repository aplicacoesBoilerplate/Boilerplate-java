package com.java.boilerplate.service.helpers;

import com.java.boilerplate.config.ROtpProperties;
import com.java.boilerplate.dto.common.RParamsSendingEmail;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.model.CUsuarioOtp;
import com.java.boilerplate.repository.IUsuarioOtpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class COtpService {
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final IUsuarioOtpRepository otpRepository;
    private final CSendEmailService sendEmailService;
    private final ROtpProperties otpProperties;
    private final COtpAttemptService otpAttemptService;
    private final SecureRandom secureRandom = new SecureRandom();

    public COtpService(
            IUsuarioOtpRepository pOtpRepository,
            CSendEmailService pSendEmailService,
            ROtpProperties pOtpProperties,
            COtpAttemptService pOtpAttemptService
    ) {
        this.otpRepository = pOtpRepository;
        this.sendEmailService = pSendEmailService;
        this.otpProperties = pOtpProperties;
        this.otpAttemptService = pOtpAttemptService;
    }

    @Transactional
    public void gerarCodigo(CUsuario pUsuario) {
        CUsuarioOtp otp = otpRepository.findByUsuario_IdUsuario(pUsuario.getIdUsuario()).orElse(null);
        if (otp != null
                && !Boolean.TRUE.equals(otp.getUtilizado())
                && otp.getExpiraEm().isAfter(LocalDateTime.now())
                && otp.getTentativas() < otpProperties.maxAttempts()) {
            return;
        }
        if (otp == null) {
            otp = new CUsuarioOtp();
        }
        String codigo = String.format("%06d", secureRandom.nextInt(1_000_000));

        otp.setUsuario(pUsuario);
        otp.setCodigo(gerarHash(pUsuario, codigo));
        otp.setExpiraEm(LocalDateTime.now().plusMinutes(otpProperties.expirationMinutes()));
        otp.setUtilizado(false);
        otp.setTentativas(0);
        otpRepository.save(otp);

        RParamsSendingEmail email = new RParamsSendingEmail(
                pUsuario.getEmail(),
                "Boilerplate - Código de recuperação",
                "otp-email",
                Map.of("nome", pUsuario.getNome(), "codigo", codigo)
        );

        sendEmailService.sendEmail(email);
    }

    public void validarCodigoSemConsumir(CUsuario pUsuario, String pCodigo) {
        otpAttemptService.validar(pUsuario.getIdUsuario(), gerarHash(pUsuario, pCodigo), false);
    }

    public void validarCodigoEConsumir(CUsuario pUsuario, String pCodigo) {
        otpAttemptService.validar(pUsuario.getIdUsuario(), gerarHash(pUsuario, pCodigo), true);
    }

    private String gerarHash(CUsuario pUsuario, String pCodigo) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(otpProperties.pepper().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] hash = mac.doFinal((pUsuario.getIdUsuario() + ":" + pCodigo).getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException pException) {
            throw new IllegalStateException("Não foi possível proteger o código de recuperação", pException);
        }
    }

}
