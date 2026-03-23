package com.java.boilerplate.config.security;

import com.java.boilerplate.config.TokensProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SwaggerSecurityConfig {
    private final TokensProperties properties;

    public SwaggerSecurityConfig(TokensProperties properties) {
        this.properties = properties;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/doc/**", "/doc/api/**", "/swagger-ui/**")
            .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @Bean
    public UserDetailsService swaggerUserDetailsService() {
        UserDetails admin = User.builder()
                .username("DeveloperArea")
                .password(String.format("{noop}%s", properties.getDocPassword()))
                .roles("SWAGGER_ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }
}
