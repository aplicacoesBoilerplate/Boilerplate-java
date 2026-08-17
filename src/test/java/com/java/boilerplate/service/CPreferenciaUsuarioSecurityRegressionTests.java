package com.java.boilerplate.service;

import com.java.boilerplate.dto.preferencias.RPreferenciaUsuario;
import com.java.boilerplate.dto.preferencias.RPreferenciasUsuario;
import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.IPreferenciaUsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CPreferenciaUsuarioSecurityRegressionTests {
    @Mock
    private IPreferenciaUsuarioRepository repository;
    @Mock
    private CAuthService authService;
    @Mock
    private jakarta.persistence.EntityManager entityManager;

    private CPreferenciaUsuarioService service;

    @BeforeEach
    void configurar() {
        service = new CPreferenciaUsuarioService(repository, authService, entityManager);
        CUsuario usuario = new CUsuario();
        usuario.setIdUsuario(10L);
        when(authService.buscarUsuarioLogado()).thenReturn(usuario);
    }

    @Test
    void loteAcimaDaQuotaDeveSerRejeitadoAntesDePersistir() {
        List<RPreferenciaUsuario> preferencias = IntStream.range(0, 21)
                .mapToObj(pIndice -> new RPreferenciaUsuario(null, "contexto", "chave-" + pIndice, "{}"))
                .toList();

        assertThatThrownBy(() -> service.salvarPreferenciasUsuarioAutenticado(new RPreferenciasUsuario(preferencias)))
                .isInstanceOf(CExceptionsSystem.class)
                .hasMessageContaining("20");
    }

    @Test
    void valorAcimaDe16KiBDeveSerRejeitado() {
        RPreferenciaUsuario preferencia = new RPreferenciaUsuario(
                null,
                "contexto",
                "chave",
                "x".repeat(16_385)
        );

        assertThatThrownBy(() -> service.salvarPreferenciaUsuarioAutenticado(preferencia))
                .isInstanceOf(CExceptionsSystem.class)
                .hasMessageContaining("16384");
    }

    @Test
    void loteComChaveDuplicadaDeveSerRejeitadoAntesDePersistir() {
        RPreferenciaUsuario repetida = new RPreferenciaUsuario(null, "contexto", "chave", "{}");

        assertThatThrownBy(() -> service.salvarPreferenciasUsuarioAutenticado(
                new RPreferenciasUsuario(List.of(repetida, repetida))))
                .isInstanceOf(CExceptionsSystem.class)
                .hasMessageContaining("duplicadas");
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).saveAll(org.mockito.ArgumentMatchers.any());
    }
}
