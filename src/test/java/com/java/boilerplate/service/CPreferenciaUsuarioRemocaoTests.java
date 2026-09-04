package com.java.boilerplate.service;

import com.java.boilerplate.cache.IRedisCache;
import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.IPreferenciaUsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CPreferenciaUsuarioRemocaoTests {
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
    }

    @Test
    void deveRemoverPreferenciaExistenteDoUsuarioAutenticadoEInvalidarSeuCache() {
        autenticarComo(10L);
        when(repository.deleteByUsuario_IdUsuarioAndContextoAndChave(10L, "filters", "usuarios")).thenReturn(1L);

        service.removerPreferenciaUsuarioAutenticado("filters", "usuarios");

        verify(repository).deleteByUsuario_IdUsuarioAndContextoAndChave(10L, "filters", "usuarios");
        verify(redisCache).remover("v1:preferencias:usuario:10");
    }

    @Test
    void ausenciaDaPreferenciaDeveSerIdempotenteEInvalidarOCacheDoUsuario() {
        autenticarComo(10L);
        when(repository.deleteByUsuario_IdUsuarioAndContextoAndChave(10L, "filters", "usuarios")).thenReturn(0L);

        assertThatCode(() -> service.removerPreferenciaUsuarioAutenticado("filters", "usuarios"))
                .doesNotThrowAnyException();

        verify(repository).deleteByUsuario_IdUsuarioAndContextoAndChave(10L, "filters", "usuarios");
        verify(redisCache).remover("v1:preferencias:usuario:10");
    }

    @Test
    void repeticaoDaRemocaoDevePermanecerIdempotente() {
        autenticarComo(10L);
        when(repository.deleteByUsuario_IdUsuarioAndContextoAndChave(10L, "filters", "usuarios"))
                .thenReturn(1L, 0L);

        service.removerPreferenciaUsuarioAutenticado("filters", "usuarios");
        service.removerPreferenciaUsuarioAutenticado("filters", "usuarios");

        verify(repository, times(2)).deleteByUsuario_IdUsuarioAndContextoAndChave(10L, "filters", "usuarios");
        verify(redisCache, times(2)).remover("v1:preferencias:usuario:10");
    }

    @Test
    void remocaoDeveUsarSomenteOIdentificadorDoUsuarioAutenticado() {
        autenticarComo(10L);

        service.removerPreferenciaUsuarioAutenticado("filters", "usuarios");

        verify(repository).deleteByUsuario_IdUsuarioAndContextoAndChave(10L, "filters", "usuarios");
        verify(repository, never()).deleteByUsuario_IdUsuarioAndContextoAndChave(20L, "filters", "usuarios");
    }

    @Test
    void contextoOuChaveInvalidosDevemSerRejeitadosAntesDeAcessarOUsuarioAutenticado() {
        assertThatThrownBy(() -> service.removerPreferenciaUsuarioAutenticado(" ", "usuarios"))
                .isInstanceOf(CExceptionsSystem.class)
                .extracting(pExcecao -> ((CExceptionsSystem) pExcecao).getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(authService, never()).buscarUsuarioLogado();
    }

    private void autenticarComo(Long pIdUsuario) {
        CUsuario usuario = new CUsuario();
        usuario.setIdUsuario(pIdUsuario);
        when(authService.buscarUsuarioLogado()).thenReturn(usuario);
    }
}
