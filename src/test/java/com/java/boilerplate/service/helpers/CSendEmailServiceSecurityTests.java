package com.java.boilerplate.service.helpers;

import com.java.boilerplate.dto.common.RParamsSendingEmail;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class CSendEmailServiceSecurityTests {
    @Test
    void falhaDeConstrucaoDoEmailNaoDeveExporDestinatario() {
        String destinatario = "usuario-sensivel@@example.com";
        org.springframework.mail.javamail.JavaMailSender mailSender = mock(org.springframework.mail.javamail.JavaMailSender.class);
        TemplateEngine templateEngine = mock(TemplateEngine.class);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        org.mockito.Mockito.doReturn("<p>codigo</p>")
                .when(templateEngine)
                .process(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(IContext.class));
        org.mockito.Mockito.when(mailSender.createMimeMessage()).thenReturn(message);
        CSendEmailService service = new CSendEmailService(mailSender, templateEngine);
        ReflectionTestUtils.setField(service, "emailFrom", "no-reply@example.com");

        assertThatThrownBy(() -> service.sendEmail(new RParamsSendingEmail(
                destinatario,
                "Recuperacao",
                "email/recuperacao",
                Map.of()
        )))
                .isInstanceOf(IllegalStateException.class)
                .satisfies(pException -> {
                    assertThat(pException.getMessage()).doesNotContain(destinatario);
                    assertThat(pException.getCause()).isNull();
                });
    }
}
