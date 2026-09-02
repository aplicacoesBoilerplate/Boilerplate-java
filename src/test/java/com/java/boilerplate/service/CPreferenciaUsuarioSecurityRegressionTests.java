package com.java.boilerplate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.java.boilerplate.cache.IRedisCache;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CPreferenciaUsuarioSecurityRegressionTests {
    @Mock
    private IPreferenciaUsuarioRepository repository;
    @Mock
    private CAuthService authService;
    @Mock
    private jakarta.persistence.EntityManager entityManager;
    @Mock
    private IRedisCache redisCache;

    private CPreferenciaUsuarioService service;

    @BeforeEach
    void configurar() {
        service = new CPreferenciaUsuarioService(repository, authService, entityManager, redisCache);
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
        verify(repository, never()).saveAll(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void cacheHitDeveEvitarConsultaAoBanco() throws Exception {
        RPreferenciasUsuario preferencias = new RPreferenciasUsuario(List.of(
                new RPreferenciaUsuario(1L, "lista", "ordenacao", "{\"campo\":\"nome\"}")));
        when(redisCache.obter("v1:preferencias:usuario:10"))
                .thenReturn(java.util.Optional.of(new ObjectMapper().writeValueAsString(preferencias)));

        assertThat(service.buscarPreferenciasUsuarioAutenticado()).isEqualTo(preferencias);

        verify(repository, never()).findTop100ByUsuario_IdUsuarioOrderByContextoAscChaveAsc(10L);
    }

    @Test
    void gravacaoDeveInvalidarCacheDoUsuario() {
        RPreferenciaUsuario preferencia = new RPreferenciaUsuario(null, "lista", "ordenacao", "{}");
        when(repository.findByUsuario_IdUsuarioAndContextoAndChave(10L, "lista", "ordenacao"))
                .thenReturn(java.util.Optional.empty());
        when(repository.countByUsuario_IdUsuario(10L)).thenReturn(0L);
        when(repository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(pInvocation -> pInvocation.getArgument(0));

        service.salvarPreferenciaUsuarioAutenticado(preferencia);

        verify(redisCache).remover("v1:preferencias:usuario:10");
    }
}
