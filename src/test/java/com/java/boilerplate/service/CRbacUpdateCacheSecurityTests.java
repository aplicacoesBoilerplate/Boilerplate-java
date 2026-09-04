package com.java.boilerplate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.java.boilerplate.cache.IRedisCache;
import com.java.boilerplate.dto.rbac.RCargoRbac;
import com.java.boilerplate.dto.rbac.RPermissaoCargoRbac;
import com.java.boilerplate.dto.rbac.RRedirecionamentoInicialRbac;
import com.java.boilerplate.enums.EComportamentoPadraoPermissao;
import com.java.boilerplate.model.CCargoRbac;
import com.java.boilerplate.model.CPermissaoCargoRbac;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.ICargoRbacRepository;
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class CRbacUpdateCacheSecurityTests {
    private static final String CHAVE_CACHE = "v1:rbac:cargo:7";

    @Mock
    private EntityManager entityManager;
    @Mock
    private ICargoRbacRepository cargoRepository;
    @Mock
    private CAuditoriaRegistroService auditoriaRegistroService;
    @Mock
    private CAutorizacaoAutoriaService autorizacaoAutoriaService;

    private CCacheMemoria cache;
    private CRbacService service;
    private CCargoRbac cargo;
    private CUsuario usuario;

    @BeforeEach
    void configurar() {
        cache = new CCacheMemoria();
        service = new CRbacService(
                entityManager, cargoRepository, auditoriaRegistroService, cache, autorizacaoAutoriaService);
        cargo = criarCargoComPermissao(true);
        usuario = new CUsuario();
        usuario.setCargo(cargo);

        when(cargoRepository.findByIdWithPermissoes(7L)).thenReturn(Optional.of(cargo));
        when(cargoRepository.findById(7L)).thenReturn(Optional.of(cargo));
        when(cargoRepository.findByPapel("GESTOR")).thenReturn(Optional.of(cargo));
        when(cargoRepository.save(cargo)).thenReturn(cargo);
        when(autorizacaoAutoriaService.autorizarGerenciamento(any(), any()))
                .thenAnswer(pInvocation -> ((Optional<?>) pInvocation.getArgument(0)).orElseThrow());
    }

    @AfterEach
    void limparSincronizacao() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void revogacaoEmUpdateDeveInvalidarCacheAquecidoSomenteAposCommit() {
        assertThat(service.usuarioPodeAcessarEndpoint(usuario, "GET", "/usuarios/10")).isTrue();
        TransactionSynchronizationManager.initSynchronization();

        service.atualizar(7L, requisicaoComPermissao(false));

        assertThat(service.usuarioPodeAcessarEndpoint(usuario, "GET", "/usuarios/10")).isTrue();
        confirmarTransacao();
        assertThat(service.usuarioPodeAcessarEndpoint(usuario, "GET", "/usuarios/10")).isFalse();
    }

    @Test
    void rollbackDeUpdateDevePreservarCacheAquecido() {
        assertThat(service.usuarioPodeAcessarEndpoint(usuario, "GET", "/usuarios/10")).isTrue();
        TransactionSynchronizationManager.initSynchronization();

        service.atualizar(7L, requisicaoComPermissao(false));
        reverterTransacao();

        assertThat(cache.contem(CHAVE_CACHE)).isTrue();
    }

    private CCargoRbac criarCargoComPermissao(boolean pLiberado) {
        CPermissaoCargoRbac permissao = new CPermissaoCargoRbac();
        permissao.setRecurso("api");
        permissao.setAcao("GET /usuarios/*");
        permissao.setLiberado(pLiberado);
        CCargoRbac novoCargo = new CCargoRbac();
        novoCargo.setIdCargo(7L);
        novoCargo.setPapel("GESTOR");
        novoCargo.setNome("Gestor");
        novoCargo.setDescricao("Cargo de teste");
        novoCargo.setComportamentoPadrao(EComportamentoPadraoPermissao.bloquear);
        novoCargo.setRedirecionamentoPath("/");
        novoCargo.setRedirecionamentoFiltros("[]");
        novoCargo.setAtivo(true);
        novoCargo.definirPermissoes(List.of(permissao));
        return novoCargo;
    }

    private RCargoRbac requisicaoComPermissao(boolean pLiberado) {
        return new RCargoRbac(
                7L,
                "GESTOR",
                "Gestor",
                null,
                "Cargo de teste",
                EComportamentoPadraoPermissao.bloquear,
                List.of(new RPermissaoCargoRbac("api", "GET /usuarios/*", pLiberado)),
                List.of(),
                new RRedirecionamentoInicialRbac("/", null, List.of()),
                true,
                null);
    }

    private void confirmarTransacao() {
        List<TransactionSynchronization> sincronizacoes = TransactionSynchronizationManager.getSynchronizations();
        sincronizacoes.forEach(pSincronizacao -> pSincronizacao.beforeCommit(false));
        sincronizacoes.forEach(TransactionSynchronization::beforeCompletion);
        sincronizacoes.forEach(TransactionSynchronization::afterCommit);
        sincronizacoes.forEach(pSincronizacao -> pSincronizacao.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        TransactionSynchronizationManager.clearSynchronization();
    }

    private void reverterTransacao() {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(pSincronizacao -> pSincronizacao.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        TransactionSynchronizationManager.clearSynchronization();
    }

    private static final class CCacheMemoria implements IRedisCache {
        private final Map<String, String> valores = new HashMap<>();

        @Override
        public Optional<String> obter(String pChave) {
            return Optional.ofNullable(valores.get(pChave));
        }

        @Override
        public void salvar(String pChave, String pValor, Duration pTtl) {
            valores.put(pChave, pValor);
        }

        @Override
        public void salvarPermanente(String pChave, String pValor) {
            valores.put(pChave, pValor);
        }

        @Override
        public void remover(String pChave) {
            valores.remove(pChave);
        }

        boolean contem(String pChave) {
            return valores.containsKey(pChave);
        }
    }
}
