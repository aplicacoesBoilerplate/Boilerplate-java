package com.java.boilerplate.service;

import com.java.boilerplate.dto.auth.RPrimeiroAcessoSenha;
import com.java.boilerplate.dto.usuarios.RUsuario;
import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.model.CCargoRbac;
import com.java.boilerplate.model.CFuncionalidadeCargoRbac;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.model.CUsuarioAtivacao;
import com.java.boilerplate.repository.ICargoRbacRepository;
import com.java.boilerplate.repository.IUsuarioAtivacaoRepository;
import com.java.boilerplate.service.helpers.CAtivacaoTokenService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest
@RecordApplicationEvents
class CAtivacaoPrimeiroAcessoConcurrencyTests {
    @Autowired
    private CUsuarioService usuarioService;
    @Autowired
    private CAtivacaoPrimeiroAcessoService ativacaoService;
    @Autowired
    private CAtivacaoTokenService ativacaoTokenService;
    @Autowired
    private IUsuarioAtivacaoRepository ativacaoRepository;
    @Autowired
    private ICargoRbacRepository cargoRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void duasChamadasConcorrentesDevemProduzirExatamenteUmConsumoBemSucedido() throws Exception {
        String token = "token-concorrente";
        CUsuario usuario = usuarioService.criarUsuarioSistema("Concorrente", "concorrente@example.com", "senha-interna", "USER", false);
        CUsuarioAtivacao ativacao = new CUsuarioAtivacao();
        ativacao.setUsuario(usuario);
        ativacao.setTokenHash(ativacaoTokenService.gerarHash(token));
        ativacao.setExpiraEm(LocalDateTime.now().plusMinutes(10));
        ativacao.setUtilizado(false);
        ativacaoRepository.saveAndFlush(ativacao);
        CyclicBarrier barreira = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<RuntimeException>> resultados = List.of(
                    executor.submit(() -> consumirAposBarreira(barreira, token)),
                    executor.submit(() -> consumirAposBarreira(barreira, token))
            );
            List<RuntimeException> resultadosConsumo = resultados.stream().map(pFuture -> {
                try {
                    return pFuture.get();
                } catch (Exception pException) {
                    throw new AssertionError("A chamada concorrente não deveria falhar fora do contrato", pException);
                }
            }).collect(Collectors.toList());
            assertThat(resultadosConsumo).containsNull();
            assertThat(resultadosConsumo.stream().filter(java.util.Objects::nonNull).toList()).hasSize(1);
            CExceptionsSystem perdedor = (CExceptionsSystem) resultadosConsumo.stream()
                    .filter(java.util.Objects::nonNull)
                    .findFirst()
                    .orElseThrow();
            assertThat(perdedor.getStatus()).isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED);
            assertThat(perdedor).hasMessage("Link de ativação inválido ou expirado");
            CUsuarioAtivacao ativacaoAtualizada = ativacaoRepository.findByUsuario_IdUsuario(usuario.getIdUsuario()).orElseThrow();
            assertThat(usuarioService.buscarEntidadePorId(usuario.getIdUsuario()).getAtivo()).isTrue();
            assertThat(ativacaoAtualizada.getUtilizado()).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void edicaoDeEmailEConsumoConcorrentesNaoPodemAtivarContaRenomeadaComTokenAntigo() throws Exception {
        CUsuario gestor = criarGestorGlobal();
        CUsuario pendente = usuarioService.criarUsuarioSistema(
                "Pendente", "corrida-email-antigo@example.com", "senha-interna", "USER", false);
        String tokenAntigo = "token-antigo-corrida-edicao-consumo";
        ativacaoRepository.saveAndFlush(criarAtivacao(pendente, tokenAntigo));
        RUsuario atual = usuarioService.buscarPorId(pendente.getIdUsuario());
        CyclicBarrier barreira = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<RuntimeException> edicao = executor.submit(() -> editarAposBarreira(barreira, gestor, atual));
            Future<RuntimeException> consumo = executor.submit(() -> consumirAposBarreira(barreira, tokenAntigo));

            RuntimeException erroEdicao = edicao.get();
            RuntimeException erroConsumo = consumo.get();
            CUsuario resultado = usuarioService.buscarEntidadePorId(pendente.getIdUsuario());
            if ("corrida-email-novo@example.com".equals(resultado.getEmail())) {
                assertThat(erroEdicao).isNull();
                assertThat(erroConsumo).isInstanceOf(CExceptionsSystem.class);
                assertThat(((CExceptionsSystem) erroConsumo).getStatus())
                        .isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED);
                assertThat(resultado.getAtivo()).isFalse();
            } else {
                assertThat(resultado.getEmail()).isEqualTo("corrida-email-antigo@example.com");
                assertThat(erroConsumo).isNull();
                assertThat(erroEdicao).isInstanceOf(CExceptionsSystem.class);
                assertThat(((CExceptionsSystem) erroEdicao).getStatus()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
                assertThat(resultado.getAtivo()).isTrue();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void reenvioEConsumoConcorrentesDevemRotacionarOuPreservarAtivacaoConsumida(ApplicationEvents pEvents) throws Exception {
        CUsuario gestor = criarGestorGlobal();
        CUsuario pendente = usuarioService.criarUsuarioSistema(
                "Pendente reenviar", "corrida-reenviar@example.com", "senha-interna", "USER", false);
        String tokenAntigo = "token-antigo-corrida-reenviar-consumo";
        String hashAntigo = ativacaoTokenService.gerarHash(tokenAntigo);
        ativacaoRepository.saveAndFlush(criarAtivacao(pendente, tokenAntigo));
        CyclicBarrier barreira = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<RuntimeException> reenvio = executor.submit(
                    () -> reenviarAposBarreira(barreira, gestor, pendente.getIdUsuario()));
            Future<RuntimeException> consumo = executor.submit(() -> consumirAposBarreira(barreira, tokenAntigo));

            RuntimeException erroReenvio = reenvio.get();
            RuntimeException erroConsumo = consumo.get();
            CUsuarioAtivacao ativacaoAtual = ativacaoRepository
                    .findByUsuario_IdUsuario(pendente.getIdUsuario()).orElseThrow();
            CUsuario usuarioAtual = usuarioService.buscarEntidadePorId(pendente.getIdUsuario());
            List<RAtivacaoEmailEvent> eventos = pEvents.stream(RAtivacaoEmailEvent.class).toList();
            if (erroReenvio == null) {
                assertThat(erroConsumo).isInstanceOf(CExceptionsSystem.class);
                assertThat(((CExceptionsSystem) erroConsumo).getStatus())
                        .isEqualTo(org.springframework.http.HttpStatus.UNAUTHORIZED);
                assertThat(usuarioAtual.getAtivo()).isFalse();
                assertThat(ativacaoAtual.getTokenHash()).isNotEqualTo(hashAntigo);
                assertThat(ativacaoAtual.getUtilizado()).isFalse();
                assertThat(eventos).hasSize(1);
            } else {
                assertThat(erroReenvio).isInstanceOf(CExceptionsSystem.class);
                assertThat(((CExceptionsSystem) erroReenvio).getStatus())
                        .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
                assertThat(erroConsumo).isNull();
                assertThat(usuarioAtual.getAtivo()).isTrue();
                assertThat(ativacaoAtual.getTokenHash()).isEqualTo(hashAntigo);
                assertThat(ativacaoAtual.getUtilizado()).isTrue();
                assertThat(eventos).isEmpty();
            }
        } finally {
            executor.shutdownNow();
        }
    }

    private RuntimeException consumirAposBarreira(CyclicBarrier pBarreira, String pToken) throws Exception {
        pBarreira.await();
        try {
            ativacaoService.consumir(new RPrimeiroAcessoSenha(pToken, "senha-nova-segura", "senha-nova-segura"));
            return null;
        } catch (RuntimeException pException) {
            return pException;
        }
    }

    private RuntimeException editarAposBarreira(CyclicBarrier pBarreira, CUsuario pGestor, RUsuario pAtual) throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(pGestor, pGestor.getPassword(), pGestor.getAuthorities()));
        pBarreira.await();
        try {
            usuarioService.editar(new RUsuario(
                    pAtual.id(), pAtual.nome(), "corrida-email-novo@example.com", pAtual.avatar(), pAtual.telefone(),
                    pAtual.notificar(), false, pAtual.papel(), pAtual.auditoria()));
            return null;
        } catch (RuntimeException pException) {
            return pException;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private RuntimeException reenviarAposBarreira(CyclicBarrier pBarreira, CUsuario pGestor, Long pIdUsuario) throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(pGestor, pGestor.getPassword(), pGestor.getAuthorities()));
        pBarreira.await();
        try {
            usuarioService.reenviarAtivacao(pIdUsuario);
            return null;
        } catch (RuntimeException pException) {
            return pException;
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private CUsuario criarGestorGlobal() {
        return new TransactionTemplate(transactionManager).execute(pStatus -> {
            CCargoRbac cargoAdmin = cargoRepository.findByPapel("ADMIN").orElseThrow();
            CFuncionalidadeCargoRbac funcionalidade = new CFuncionalidadeCargoRbac();
            funcionalidade.setFuncionalidade("gerenciarRegistros");
            funcionalidade.setLiberado(true);
            cargoAdmin.definirFuncionalidades(List.of(funcionalidade));
            cargoRepository.saveAndFlush(cargoAdmin);
            return usuarioService.criarUsuarioSistema(
                    "Gestor", "gestor-corrida@example.com", "senha-interna", "ADMIN", true);
        });
    }

    private CUsuarioAtivacao criarAtivacao(CUsuario pUsuario, String pToken) {
        CUsuarioAtivacao ativacao = new CUsuarioAtivacao();
        ativacao.setUsuario(pUsuario);
        ativacao.setTokenHash(ativacaoTokenService.gerarHash(pToken));
        ativacao.setExpiraEm(LocalDateTime.now().plusMinutes(10));
        ativacao.setUtilizado(false);
        return ativacao;
    }
}
