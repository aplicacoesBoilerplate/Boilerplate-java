package com.java.boilerplate.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class CGoogleOAuthConfiguration {
    @Bean
    public GoogleIdTokenVerifier googleIdTokenVerifier(RTokensProperties pProperties) {
        return new GoogleIdTokenVerifier.Builder(Utils.getDefaultTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(pProperties.googleClientId()))
                .build();
    }
}
