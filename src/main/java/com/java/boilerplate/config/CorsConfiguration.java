package com.java.boilerplate.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfiguration implements WebMvcConfigurer {

    private final TokensProperties props;

    public CorsConfiguration(TokensProperties props) {
        this.props = props;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // aplica em todos os endpoints
                .allowedOrigins("http://localhost:9000", props.getHost()) // Hosts que estão permitidos fazer requisição
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH") // métodos permitidos
                .allowedHeaders("*") // cabeçalhos permitidos
                .allowCredentials(true); // permite envio de cookies/autenticação
    }

}
