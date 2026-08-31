package com.java.boilerplate.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CConfigurationSecurityRegressionTests {
    private static Validator validator;

    @BeforeAll
    static void criarValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void chaveJweInvalidaDeveSerRejeitadaAntesDaAplicacaoIniciar() {
        RTokensProperties properties = new RTokensProperties(
                "chave-invalida",
                "boilerplate-java-api",
                720L,
                ""
        );

        assertThat(validator.validate(properties))
                .extracting(pViolation -> pViolation.getPropertyPath().toString())
                .contains("encryptionKeyValida", "accessTokenMinutes");
    }

    @Test
    void credenciaisPublicasDaDocumentacaoDevemSerRejeitadas() {
        RDocumentacaoProperties properties = new RDocumentacaoProperties(
                true,
                "DeveloperArea",
                "boilerplate",
                ""
        );

        assertThat(validator.validate(properties)).isNotEmpty();
    }

    @Test
    void documentacaoDesabilitadaNaoDeveExigirCredenciais() {
        RDocumentacaoProperties properties = new RDocumentacaoProperties(
                false,
                "",
                "",
                ""
        );

        assertThat(validator.validate(properties)).isEmpty();
    }
}
