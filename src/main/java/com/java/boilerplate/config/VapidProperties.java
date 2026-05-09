package com.java.boilerplate.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "push.vapid")
public class VapidProperties {
    private String publicKey;
    private String privateKey;
    private String subject;
}
