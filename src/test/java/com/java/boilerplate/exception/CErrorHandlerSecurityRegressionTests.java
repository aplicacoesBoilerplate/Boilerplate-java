package com.java.boilerplate.exception;

import com.java.boilerplate.config.RAppProperties;
import com.java.boilerplate.dto.common.RErro;
import com.java.boilerplate.model.CLogErro;
import com.java.boilerplate.repository.ILogErroRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CErrorHandlerSecurityRegressionTests {
    @Mock
    private ILogErroRepository logErroRepository;

    private CErrorHandler errorHandler;

    @BeforeEach
    void configurar() {
        errorHandler = new CErrorHandler(
                logErroRepository,
                new RAppProperties("http://localhost", "http://localhost", false, 1000)
        );
    }

    @Test
    void erroInesperadoDeveRetornarEPersistirSomenteMensagemGenerica() {
        ResponseEntity<RErro> response = errorHandler.handlerException(
                new RuntimeException("jdbc:mysql://db-interno/schema?password=segredo")
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().mensagem()).isEqualTo("Erro interno ao processar a solicitação");
        ArgumentCaptor<CLogErro> captor = ArgumentCaptor.forClass(CLogErro.class);
        verify(logErroRepository).save(captor.capture());
        assertThat(captor.getValue().getMensagem()).isEqualTo("Erro interno ao processar a solicitação");
    }

    @Test
    void erroDeClienteNaoDeveAmplificarLogPersistente() {
        errorHandler.handlerExceptionsSystem(new CExceptionsSystem("Entrada inválida", HttpStatus.BAD_REQUEST));

        verify(logErroRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void credenciaisInvalidasDevemRetornar401SemLogPersistente() {
        ResponseEntity<RErro> response = errorHandler.handlerAuthenticationException(
                new BadCredentialsException("detalhe sensivel")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().mensagem()).isEqualTo("Credenciais inválidas");
        verify(logErroRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void jsonInvalidoDeveRetornar400SemLogPersistente() {
        ResponseEntity<RErro> response = errorHandler.handlerClientParsingException(
                new HttpMessageNotReadableException("json inválido", new MockHttpInputMessage(new byte[0]))
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(logErroRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void limiteDeRequisicoesDeveInformarQuandoTentarNovamente() {
        ResponseEntity<RErro> response = errorHandler.handlerExceptionsSystem(
                new CExceptionsSystem("Muitas tentativas", HttpStatus.TOO_MANY_REQUESTS, 60)
        );

        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("60");
        verify(logErroRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void proibicaoDeveSerGenericaESemTraceMesmoQuandoExposicaoEstaAtiva() {
        errorHandler = new CErrorHandler(
                logErroRepository,
                new RAppProperties("http://localhost", "http://localhost", true, 1000)
        );

        ResponseEntity<RErro> response = errorHandler.handlerExceptionsSystem(
                new CExceptionsSystem(
                        "Registro 42 criado por autor@example.com",
                        HttpStatus.FORBIDDEN,
                        "REGISTRO_ALHEIO",
                        Map.of("id", 42L, "papel", "ADMIN"))
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().mensagem()).isEqualTo("Operação não autorizada");
        assertThat(response.getBody().codigo()).isNull();
        assertThat(response.getBody().dados()).isNull();
        assertThat(response.getBody().trace()).isNull();
        verify(logErroRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void accessDeniedDoSpringSecurityDeveRetornar403Sanitizado() throws Exception {
        MockMvc mockMvc = criarMockMvcComExposicaoDeTrace();

        mockMvc.perform(get("/negado/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Operação não autorizada"))
                .andExpect(jsonPath("$.codigo").doesNotExist())
                .andExpect(jsonPath("$.dados").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());

        verify(logErroRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void authorizationDeniedDoPreAuthorizeDeveRetornar403Sanitizado() throws Exception {
        MockMvc mockMvc = criarMockMvcComExposicaoDeTrace();

        mockMvc.perform(get("/negado/authorization-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Operação não autorizada"))
                .andExpect(jsonPath("$.codigo").doesNotExist())
                .andExpect(jsonPath("$.dados").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist());

        verify(logErroRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void persistenciaDeErrosDeveRemoverRegistrosQueExcedemORetencao() {
        org.mockito.Mockito.when(logErroRepository.count()).thenReturn(1001L);
        CLogErro antigo = new CLogErro();
        org.mockito.Mockito.when(logErroRepository.findAll(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(antigo)));

        errorHandler.handlerException(new RuntimeException("falha interna"));

        verify(logErroRepository).deleteAllInBatch(List.of(antigo));
    }

    private MockMvc criarMockMvcComExposicaoDeTrace() {
        errorHandler = new CErrorHandler(
                logErroRepository,
                new RAppProperties("http://localhost", "http://localhost", true, 1000));
        return MockMvcBuilders.standaloneSetup(new CNegacaoController())
                .setControllerAdvice(errorHandler)
                .build();
    }

    @RestController
    private static class CNegacaoController {
        @GetMapping("/negado/access-denied")
        void accessDenied() {
            throw new AccessDeniedException("detalhe sensível de autorização");
        }

        @GetMapping("/negado/authorization-denied")
        void authorizationDenied() {
            throw new AuthorizationDeniedException("detalhe sensível do @PreAuthorize");
        }
    }
}
