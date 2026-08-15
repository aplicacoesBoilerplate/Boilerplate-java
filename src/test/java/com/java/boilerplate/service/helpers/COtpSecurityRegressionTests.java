package com.java.boilerplate.service.helpers;

import com.java.boilerplate.config.ROtpProperties;
import com.java.boilerplate.dto.common.RParamsSendingEmail;
import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.model.CUsuarioOtp;
import com.java.boilerplate.repository.IUsuarioOtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class COtpSecurityRegressionTests {
    @Mock
    private IUsuarioOtpRepository otpRepository;
    @Mock
    private CSendEmailService sendEmailService;

    private COtpService otpService;
    private CUsuario usuario;

    @BeforeEach
    void configurar() {
        otpService = new COtpService(
                otpRepository,
                sendEmailService,
                new ROtpProperties("test-only-otp-pepper-with-at-least-32-characters", 10, 5),
                new COtpAttemptService(
                        otpRepository,
                        new ROtpProperties("test-only-otp-pepper-with-at-least-32-characters", 10, 5)
                )
        );
        usuario = new CUsuario();
        usuario.setIdUsuario(42L);
        usuario.setNome("Titular");
        usuario.setEmail("titular@example.com");
    }

    @Test
    void gerarCodigoDevePersistirSomenteRepresentacaoNaoReversivel() {
        when(otpRepository.findByUsuario_IdUsuario(42L)).thenReturn(Optional.empty());
        when(otpRepository.save(any(CUsuarioOtp.class))).thenAnswer(pInvocation -> pInvocation.getArgument(0));

        otpService.gerarCodigo(usuario);

        ArgumentCaptor<CUsuarioOtp> otpCaptor = ArgumentCaptor.forClass(CUsuarioOtp.class);
        ArgumentCaptor<RParamsSendingEmail> emailCaptor = ArgumentCaptor.forClass(RParamsSendingEmail.class);
        verify(otpRepository).save(otpCaptor.capture());
        verify(sendEmailService).sendEmail(emailCaptor.capture());

        String codigoEnviado = String.valueOf(emailCaptor.getValue().variables().get("codigo"));
        assertThat(codigoEnviado).matches("\\d{6}");
        assertThat(otpCaptor.getValue().getCodigo())
                .hasSize(64)
                .isNotEqualTo(codigoEnviado);
    }

    @Test
    void excederTentativasInvalidasDeveBloquearMesmoOCodigoCorreto() {
        when(otpRepository.findByUsuario_IdUsuario(42L)).thenReturn(Optional.empty());
        when(otpRepository.save(any(CUsuarioOtp.class))).thenAnswer(pInvocation -> pInvocation.getArgument(0));
        otpService.gerarCodigo(usuario);

        ArgumentCaptor<CUsuarioOtp> otpCaptor = ArgumentCaptor.forClass(CUsuarioOtp.class);
        ArgumentCaptor<RParamsSendingEmail> emailCaptor = ArgumentCaptor.forClass(RParamsSendingEmail.class);
        verify(otpRepository).save(otpCaptor.capture());
        verify(sendEmailService).sendEmail(emailCaptor.capture());
        CUsuarioOtp otp = otpCaptor.getValue();
        String codigoCorreto = String.valueOf(emailCaptor.getValue().variables().get("codigo"));
        when(otpRepository.findByUsuario_IdUsuario(42L)).thenReturn(Optional.of(otp));

        for (int tentativa = 0; tentativa < 5; tentativa++) {
            assertThatThrownBy(() -> otpService.validarCodigoSemConsumir(usuario, "000000"))
                    .isInstanceOf(CExceptionsSystem.class);
        }

        assertThatThrownBy(() -> otpService.validarCodigoSemConsumir(usuario, codigoCorreto))
                .isInstanceOf(CExceptionsSystem.class)
                .hasMessage("Código de recuperação inválido ou expirado");
        assertThat(otp.getTentativas()).isEqualTo(5);
        assertThat(otp.getUtilizado()).isTrue();
    }

    @Test
    void codigoAindaValidoNaoDeveSerSubstituidoNemReenviado() {
        CUsuarioOtp existente = new CUsuarioOtp();
        existente.setUsuario(usuario);
        existente.setCodigo("a".repeat(64));
        existente.setExpiraEm(java.time.LocalDateTime.now().plusMinutes(5));
        existente.setUtilizado(false);
        existente.setTentativas(0);
        when(otpRepository.findByUsuario_IdUsuario(42L)).thenReturn(Optional.of(existente));

        otpService.gerarCodigo(usuario);

        org.mockito.Mockito.verify(otpRepository, org.mockito.Mockito.never()).save(any());
        org.mockito.Mockito.verify(sendEmailService, org.mockito.Mockito.never()).sendEmail(any());
    }
}
