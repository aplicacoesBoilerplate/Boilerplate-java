package com.java.boilerplate.service;

import com.java.boilerplate.config.RAppProperties;
import com.java.boilerplate.dto.common.RParamsSendingEmail;
import com.java.boilerplate.service.helpers.CSendEmailService;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class CAtivacaoEmailListener {
    private final CSendEmailService sendEmailService;
    private final RAppProperties appProperties;

    public CAtivacaoEmailListener(CSendEmailService pSendEmailService, RAppProperties pAppProperties) {
        this.sendEmailService = pSendEmailService;
        this.appProperties = pAppProperties;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviar(RAtivacaoEmailEvent pEvent) {
        String link = appProperties.frontendUrl() + "/primeiro-acesso#token=" + pEvent.token();
        sendEmailService.sendEmail(new RParamsSendingEmail(
                pEvent.email(),
                "Ative sua conta",
                "ativacao-primeiro-acesso",
                Map.of("nome", pEvent.nome(), "link", link, "ttlMinutos", pEvent.expirationMinutes())
        ));
    }
}
