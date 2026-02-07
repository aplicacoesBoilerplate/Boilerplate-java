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
    private String payGateway;
    private String host;
    private String uploadDir;
}
