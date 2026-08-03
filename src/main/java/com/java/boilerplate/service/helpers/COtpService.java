package com.java.boilerplate.service.helpers;

import com.java.boilerplate.dto.common.RParamsSendingEmail;
import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.model.CUsuarioOtp;
import com.java.boilerplate.repository.IUsuarioOtpRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class COtpService {
    private final IUsuarioOtpRepository otpRepository;
    private final CSendEmailService sendEmailService;

    public COtpService(IUsuarioOtpRepository pOtpRepository, CSendEmailService pSendEmailService) {
        this.otpRepository = pOtpRepository;
        this.sendEmailService = pSendEmailService;
    }

    @Transactional
    public void gerarCodigo(CUsuario pUsuario) {
        CUsuarioOtp otp =
                otpRepository.findByUsuario_IdUsuario(pUsuario.getIdUsuario()).orElse(new CUsuarioOtp());
        String codigo = String.format("%06d", new Random().nextInt(1_000_000));

        otp.setUsuario(pUsuario);
        otp.setCodigo(codigo);
        otp.setExpiraEm(LocalDateTime.now().plusMinutes(10));
        otp.setUtilizado(false);
        otpRepository.save(otp);

        RParamsSendingEmail email = new RParamsSendingEmail(
                pUsuario.getEmail(),
                "Boilerplate - Código de recuperação",
                "otp-email",
                Map.of("nome", pUsuario.getNome(), "codigo", codigo));

        sendEmailService.sendEmail(email);
    }

    @Transactional(readOnly = true)
    public void validarCodigoSemConsumir(CUsuario pUsuario, String pCodigo) {
        CUsuarioOtp otp = buscarOtpValido(pUsuario, pCodigo);
        if (Boolean.TRUE.equals(otp.getUtilizado())) {
            throw new CExceptionsSystem("Código de recuperação já utilizado", HttpStatus.UNAUTHORIZED);
        }
    }

    @Transactional
    public void validarCodigoEConsumir(CUsuario pUsuario, String pCodigo) {
        CUsuarioOtp otp = buscarOtpValido(pUsuario, pCodigo);
        if (Boolean.TRUE.equals(otp.getUtilizado())) {
            throw new CExceptionsSystem("Código de recuperação já utilizado", HttpStatus.UNAUTHORIZED);
        }

        otp.setUtilizado(true);
        otpRepository.save(otp);
    }

    private CUsuarioOtp buscarOtpValido(CUsuario pUsuario, String pCodigo) {
        CUsuarioOtp otp = otpRepository
                .findByUsuario_IdUsuario(pUsuario.getIdUsuario())
                .orElseThrow(
                        () -> new CExceptionsSystem("Código de recuperação não encontrado", HttpStatus.UNAUTHORIZED));

        if (!otp.getCodigo().equals(pCodigo)) {
            throw new CExceptionsSystem("Código de recuperação inválido", HttpStatus.UNAUTHORIZED);
        }

        if (otp.getExpiraEm().isBefore(LocalDateTime.now())) {
            throw new CExceptionsSystem("Código de recuperação expirado", HttpStatus.UNAUTHORIZED);
        }

        return otp;
    }
}
