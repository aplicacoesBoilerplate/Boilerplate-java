package com.java.boilerplate.service;

import com.java.boilerplate.config.RAtivacaoProperties;
import com.java.boilerplate.config.security.CTokenService;
import com.java.boilerplate.dto.auth.RPrimeiroAcessoSenha;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.model.CUsuarioAtivacao;
import com.java.boilerplate.repository.IUsuarioAtivacaoRepository;
import com.java.boilerplate.service.helpers.CAtivacaoTokenService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CAtivacaoPrimeiroAcessoServiceTests {
    @Mock
    private IUsuarioAtivacaoRepository ativacaoRepository;
    @Mock
    private CTokenService tokenService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void emissaoDevePersistirSomenteHashHmacEPublicarTokenSomenteNoEventoDeEmail() {
        CAtivacaoTokenService tokenServiceAtivacao = new CAtivacaoTokenService(properties());
        CAtivacaoPrimeiroAcessoService service = service(tokenServiceAtivacao);
        CUsuario usuario = usuario(false);
        when(ativacaoRepository.findByUsuario_IdUsuario(9L)).thenReturn(Optional.empty());

        service.emitir(usuario);

        ArgumentCaptor<CUsuarioAtivacao> ativacaoCaptor = ArgumentCaptor.forClass(CUsuarioAtivacao.class);
        ArgumentCaptor<RAtivacaoEmailEvent> eventoCaptor = ArgumentCaptor.forClass(RAtivacaoEmailEvent.class);
        verify(ativacaoRepository).save(ativacaoCaptor.capture());
        verify(eventPublisher).publishEvent(eventoCaptor.capture());
        assertThat(ativacaoCaptor.getValue().getTokenHash()).hasSize(64).isNotEqualTo(eventoCaptor.getValue().token());
        assertThat(eventoCaptor.getValue().token()).doesNotContain("senha");
        assertThat(ativacaoCaptor.getValue().getExpiraEm()).isAfter(LocalDateTime.now().plusMinutes(59));
    }

    @Test
    void consumoValidoDeveAtivarTrocarSenhaMarcarUsoERevogarSessoes() {
        CAtivacaoTokenService tokenServiceAtivacao = new CAtivacaoTokenService(properties());
        CAtivacaoPrimeiroAcessoService service = service(tokenServiceAtivacao);
        CUsuario usuario = usuario(false);
        CUsuarioAtivacao ativacao = new CUsuarioAtivacao();
        ativacao.setUsuario(usuario);
        ativacao.setTokenHash(tokenServiceAtivacao.gerarHash("token-valido"));
        ativacao.setExpiraEm(LocalDateTime.now().plusMinutes(10));
        ativacao.setUtilizado(false);
        when(ativacaoRepository.findByTokenHashForUpdate(ativacao.getTokenHash())).thenReturn(Optional.of(ativacao));

        service.consumir(new RPrimeiroAcessoSenha("token-valido", "senha-nova-segura", "senha-nova-segura"));

        assertThat(usuario.getAtivo()).isTrue();
        assertThat(new BCryptPasswordEncoder().matches("senha-nova-segura", usuario.getSenha())).isTrue();
        assertThat(ativacao.getUtilizado()).isTrue();
        assertThat(ativacao.getUtilizadoEm()).isNotNull();
        verify(tokenService).revogarSessoesUsuario(9L);
    }

    private CAtivacaoPrimeiroAcessoService service(CAtivacaoTokenService pTokenServiceAtivacao) {
        return new CAtivacaoPrimeiroAcessoService(
                ativacaoRepository, pTokenServiceAtivacao, properties(), new BCryptPasswordEncoder(), tokenService, eventPublisher);
    }

    private RAtivacaoProperties properties() {
        return new RAtivacaoProperties("test-only-activation-pepper-with-at-least-32-characters", 60);
    }

    private CUsuario usuario(boolean pAtivo) {
        CUsuario usuario = new CUsuario();
        usuario.setIdUsuario(9L);
        usuario.setNome("Titular");
        usuario.setEmail("titular@example.com");
        usuario.setAtivo(pAtivo);
        usuario.setSenha(new BCryptPasswordEncoder().encode("senha-interna"));
        return usuario;
    }
}
