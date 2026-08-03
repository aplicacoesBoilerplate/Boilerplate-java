package com.java.boilerplate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @description Record dos atributos do bloco 'documentação' no application.yml.
 */
@ConfigurationProperties(prefix = "documentacao")
public record RDocumentacaoProperties(String usuario, String senha, String senhaHash) {}
