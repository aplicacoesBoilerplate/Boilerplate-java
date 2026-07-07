package com.java.boilerplate.config;

import com.java.boilerplate.model.CUsuario;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * @description Bean de configuração atuando como um provider para tratamento da autenticação no contexto atual.
 * @return Retorna um id vinculado ao usuário autenticado ou um Optional.empty
 */
@Configuration
@EnableJpaAuditing
public class CAuditoriaConfiguration {
    /** @description Implementação do Bean de configuração para retornar o id do usuário autenticado. */
    @Bean
    public AuditorAware<Long> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof CUsuario usuario && usuario.getIdUsuario() != null) {
                return Optional.of(usuario.getIdUsuario());
            }

            return Optional.empty();
        };
    }
}
