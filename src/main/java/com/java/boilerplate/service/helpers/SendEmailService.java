package com.java.boilerplate.service.helpers;

import com.java.boilerplate.dto.DTOParamsSendingEmail;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class SendEmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String emailFrom;

    public SendEmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendEmail(DTOParamsSendingEmail request) {
        try {
            Context context = new Context();
            if (request.variables() != null) {
                context.setVariables(request.variables());
            }

            String htmlContent = templateEngine.process(request.templateName(), context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(emailFrom);
            helper.setTo(request.to());
            helper.setSubject(request.subject());
            helper.setText(htmlContent, true);

            ClassPathResource logo = new ClassPathResource("mail/TZ_LogoApp.jpg");
            helper.addInline("appLogo", logo);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException(String.format("Erro ao enviar o e-mail para %s. %s", request.to(), e));
        }
    }
}
