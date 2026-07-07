package com.java.boilerplate.service.helpers;

import com.java.boilerplate.dto.common.RParamsSendingEmail;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class CSendEmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String emailFrom;

    public CSendEmailService(JavaMailSender pMailSender, TemplateEngine pTemplateEngine) {
        this.mailSender = pMailSender;
        this.templateEngine = pTemplateEngine;
    }

    public void sendEmail(RParamsSendingEmail pRequest) {
        try {
            Context context = new Context();
            if (pRequest.variables() != null) {
                context.setVariables(pRequest.variables());
            }

            String htmlContent = templateEngine.process(pRequest.templateName(), context);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailFrom);
            helper.setTo(pRequest.to());
            helper.setSubject(pRequest.subject());
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException pException) {
            throw new IllegalStateException(String.format("Erro ao enviar e-mail para %s", pRequest.to()), pException);
        }
    }
}
