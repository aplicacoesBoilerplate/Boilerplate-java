package com.java.boilerplate.controller.webhooks;

import com.java.boilerplate.dto.infinitepay.DTOInfinitePayWebhook;
import com.java.boilerplate.service.WebhooksService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class WebHooksHandler {
    private final WebhooksService webhooksService;

    public WebHooksHandler(WebhooksService webhooksService) {
        this.webhooksService = webhooksService;
    }

    public ResponseEntity<Void> infinitePayResponsePayment(DTOInfinitePayWebhook payload) {
        webhooksService.infinitePayResponsePayment(payload);
        return ResponseEntity.ok().build();
    }
}
