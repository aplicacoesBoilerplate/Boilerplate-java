package com.java.boilerplate.controller.webhooks;

import com.java.boilerplate.dto.infinitepay.DTOInfinitePayWebhook;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhook")
public class WebhooksController {
    private final WebHooksHandler webHooksHandler;

    public WebhooksController(WebHooksHandler webHooksHandler) {
        this.webHooksHandler = webHooksHandler;
    }

    @PostMapping("/infinitePay")
    public ResponseEntity<Void> infinitePay(
            @RequestBody DTOInfinitePayWebhook payload
    ) { return webHooksHandler.infinitePay(payload); }
}
