package com.java.boilerplate.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.java.boilerplate.dto.rbac.RCargoRbac;
import com.java.boilerplate.dto.rbac.RFuncionalidadeCargoRbac;
import com.java.boilerplate.dto.rbac.RRedirecionamentoInicialRbac;
import com.java.boilerplate.enums.EComportamentoPadraoPermissao;
import com.java.boilerplate.model.CCargoRbac;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.ICargoRbacRepository;
import com.java.boilerplate.repository.IUsuarioRepository;
import jakarta.persistence.EntityManager;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class CRbacFuncionalidadeCompatibilityTests {
    private static final String FUNCIONALIDADE_CANONICA = "gerenciarRegistros";
    private static final String FUNCIONALIDADE_LEGADA = "gerenciarRegistrosOutros";
    private static final String OUTRA_FUNCIONALIDADE = "exportarRelatorios";
    private static final AtomicLong SEQUENCIA = new AtomicLong();

    @Autowired
    private CRbacService rbacService;

    @Autowired
    private ICargoRbacRepository cargoRepository;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void payloadVueDevePersistirCanonicoExibirAliasEPermitirRevogacaoNoResave() {
        CUsuario ator = criarEAutenticarAtor();

        RCargoRbac criado = rbacService.cadastrar(requisicaoCargo(
                null,
                List.of(
                        new RFuncionalidadeCargoRbac(FUNCIONALIDADE_LEGADA, true),
                        new RFuncionalidadeCargoRbac(OUTRA_FUNCIONALIDADE, true))));
        Map<String, Boolean> persistidasNaCriacao = mapearFuncionalidadesPersistidas(criado.id());

        autenticar(ator);
        RCargoRbac revogado = rbacService.atualizar(
                criado.id(),
                requisicaoCargo(
                        criado.id(),
                        List.of(
                                new RFuncionalidadeCargoRbac(FUNCIONALIDADE_LEGADA, false),
                                new RFuncionalidadeCargoRbac(OUTRA_FUNCIONALIDADE, true))));
        Map<String, Boolean> persistidasNaRevogacao = mapearFuncionalidadesPersistidas(criado.id());

        assertFuncionalidadeExterna(criado, FUNCIONALIDADE_LEGADA, true);
        assertFuncionalidadeExterna(criado, OUTRA_FUNCIONALIDADE, true);
        assertThat(criado.funcionalidades()).noneMatch(
                pItem -> FUNCIONALIDADE_CANONICA.equals(pItem.funcionalidade()));
        assertFuncionalidadeExterna(revogado, FUNCIONALIDADE_LEGADA, false);
        assertFuncionalidadeExterna(revogado, OUTRA_FUNCIONALIDADE, true);
        assertThat(persistidasNaCriacao)
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        FUNCIONALIDADE_CANONICA, true,
                        OUTRA_FUNCIONALIDADE, true));
        assertThat(persistidasNaRevogacao)
                .containsExactlyInAnyOrderEntriesOf(Map.of(
                        FUNCIONALIDADE_CANONICA, false,
                        OUTRA_FUNCIONALIDADE, true));
    }

    @Test
    void funcionalidadeCanonicaExplicitaDevePrevalecerSobreAliasEmQualquerOrdem() {
        criarEAutenticarAtor();

        RCargoRbac canonicoPorUltimo = rbacService.cadastrar(requisicaoCargo(
                null,
                List.of(
                        new RFuncionalidadeCargoRbac(FUNCIONALIDADE_LEGADA, true),
                        new RFuncionalidadeCargoRbac(FUNCIONALIDADE_CANONICA, false))));
        RCargoRbac canonicoPrimeiro = rbacService.cadastrar(requisicaoCargo(
                null,
                List.of(
                        new RFuncionalidadeCargoRbac(FUNCIONALIDADE_CANONICA, true),
                        new RFuncionalidadeCargoRbac(FUNCIONALIDADE_LEGADA, false))));
        Map<String, Boolean> persistidasCanonicoPorUltimo =
                mapearFuncionalidadesPersistidas(canonicoPorUltimo.id());
        Map<String, Boolean> persistidasCanonicoPrimeiro =
                mapearFuncionalidadesPersistidas(canonicoPrimeiro.id());

        assertFuncionalidadeExterna(canonicoPorUltimo, FUNCIONALIDADE_LEGADA, false);
        assertFuncionalidadeExterna(canonicoPrimeiro, FUNCIONALIDADE_LEGADA, true);
        assertThat(canonicoPorUltimo.funcionalidades()).hasSize(1);
        assertThat(canonicoPrimeiro.funcionalidades()).hasSize(1);
        assertThat(persistidasCanonicoPorUltimo)
                .containsExactlyEntriesOf(Map.of(FUNCIONALIDADE_CANONICA, false));
        assertThat(persistidasCanonicoPrimeiro)
                .containsExactlyEntriesOf(Map.of(FUNCIONALIDADE_CANONICA, true));
    }

    private CUsuario criarEAutenticarAtor() {
        SecurityContextHolder.clearContext();
        CUsuario raiz = usuarioRepository.findById(1L).orElseGet(this::criarRaiz);
        autenticar(raiz);
        return raiz;
    }

    private CUsuario criarRaiz() {
        CCargoRbac cargoAdmin = cargoRepository.findByPapel("ADMIN").orElseThrow();
        entityManager.createNativeQuery("""
                        INSERT INTO usuarios (id_usuario, nome, email, senha, notificar, ativo, id_cargo, criado_em)
                        VALUES (1, 'Raiz', 'root-compat@example.com', 'senha-codificada', false, true, :idCargo, CURRENT_TIMESTAMP)
                        """)
                .setParameter("idCargo", cargoAdmin.getIdCargo())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        return usuarioRepository.findById(1L).orElseThrow();
    }

    private void autenticar(CUsuario pUsuario) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        pUsuario, pUsuario.getPassword(), pUsuario.getAuthorities()));
    }

    private RCargoRbac requisicaoCargo(Long pId, List<RFuncionalidadeCargoRbac> pFuncionalidades) {
        long sequencia = SEQUENCIA.incrementAndGet();
        return new RCargoRbac(
                pId,
                "COMPAT_" + sequencia,
                "Cargo compatível " + sequencia,
                "mdi-shield-account",
                "Cargo de teste de compatibilidade",
                EComportamentoPadraoPermissao.bloquear,
                List.of(),
                pFuncionalidades,
                new RRedirecionamentoInicialRbac("/", null, List.of()),
                true,
                false,
                null);
    }

    private void assertFuncionalidadeExterna(
            RCargoRbac pCargo, String pFuncionalidade, boolean pLiberado) {
        assertThat(pCargo.funcionalidades())
                .filteredOn(pItem -> pFuncionalidade.equals(pItem.funcionalidade()))
                .singleElement()
                .extracting(RFuncionalidadeCargoRbac::liberado)
                .isEqualTo(pLiberado);
    }

    private Map<String, Boolean> mapearFuncionalidadesPersistidas(Long pIdCargo) {
        entityManager.flush();
        entityManager.clear();
        CCargoRbac persistido = cargoRepository.findById(pIdCargo).orElseThrow();
        Map<String, Boolean> funcionalidades = new LinkedHashMap<>();
        persistido.getFuncionalidades().forEach(
                pItem -> funcionalidades.put(pItem.getFuncionalidade(), pItem.getLiberado()));
        return funcionalidades;
    }
}
