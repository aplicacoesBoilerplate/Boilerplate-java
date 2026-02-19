package com.java.boilerplate.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "tokens")
@Getter
@Setter
public class TokensProperties {
    private String secret;
    private String host;
    private String uploadDir;
    private String portFront;
    private String infinitePayEndpoint;
    private String infinitePayHandle;
    private String infinitePayRedirectUrl;
    private String infinitePayWebhookUrl;
}
