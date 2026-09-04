package com.java.boilerplate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.java.boilerplate.dto.rbac.RCargoRbac;
import com.java.boilerplate.dto.rbac.RFuncionalidadeCargoRbac;
import com.java.boilerplate.dto.rbac.RPermissaoCargoRbac;
import com.java.boilerplate.dto.rbac.RRedirecionamentoInicialRbac;
import com.java.boilerplate.dto.usuarios.RUsuario;
import com.java.boilerplate.enums.EComportamentoPadraoPermissao;
import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.model.CCargoRbac;
import com.java.boilerplate.model.CFuncionalidadeCargoRbac;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.ICargoRbacRepository;
import com.java.boilerplate.repository.IUsuarioRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class CRbacDoisNiveisSecurityTests {
    private static final AtomicLong SEQUENCIA = new AtomicLong();

    @Autowired
    private CRbacService rbacService;

    @Autowired
    private CUsuarioService usuarioService;

    @Autowired
    private ICargoRbacRepository cargoRepository;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void usuarioFinalNaoPodeCriarCargoMesmoQuandoChamadoAposAutorizacaoDeRota() {
        criarRaiz();
        CUsuario usuarioFinal = usuarioService.criarUsuarioSistema(
                "Cliente final", "cliente-final-" + SEQUENCIA.incrementAndGet() + "@example.com", "senha-segura", "USER", true);
        assertThat(usuarioFinal.getCargo().getDestinadoClienteFinal()).isTrue();
        liberarGerenciamentoGlobal(usuarioFinal.getCargo());
        autenticar(usuarioFinal);

        assertNegado(() -> rbacService.cadastrar(requisicaoCargo("CARGO_NEGADO")));
    }

    @Test
    void administradorGestorNaoRaizCriaSomenteCargosEUsuariosFinais() {
        CUsuario raiz = criarRaiz();
        liberarGerenciamentoGlobal(raiz.getCargo());
        CUsuario gestor = usuarioService.criarUsuarioSistema(
                "Gestor", "gestor-" + SEQUENCIA.incrementAndGet() + "@example.com", "senha-segura", "ADMIN", true);
        autenticar(gestor);

        RCargoRbac cargoFinal = rbacService.cadastrar(requisicaoCargo(
                "FINAL_PERMITIDO", true, EComportamentoPadraoPermissao.bloquear, List.of(), List.of()));
        RUsuario usuarioFinal = usuarioService.cadastrar(requisicaoUsuario("usuario-final", cargoFinal.papel()));

        assertThat(cargoFinal.destinadoClienteFinal()).isTrue();
        assertThat(usuarioFinal.papel()).isEqualTo(cargoFinal.papel());
        assertNegado(() -> rbacService.cadastrar(requisicaoCargo(
                "GESTOR_NEGADO", false, EComportamentoPadraoPermissao.bloquear, List.of(), List.of())));
        assertNegado(() -> usuarioService.cadastrar(requisicaoUsuario("usuario-gestor", "ADMIN")));
    }

    @Test
    void raizPodeCriarCargoGestorEAtribuirQualquerCargoAtivo() {
        CUsuario raiz = criarRaiz();
        autenticar(raiz);

        RCargoRbac gestor = rbacService.cadastrar(requisicaoCargo(
                "GESTOR_RAIZ", false, EComportamentoPadraoPermissao.liberar, List.of(), List.of()));
        RUsuario usuarioGestor = usuarioService.cadastrar(requisicaoUsuario("usuario-gestor-raiz", gestor.papel()));

        assertThat(gestor.destinadoClienteFinal()).isFalse();
        assertThat(usuarioGestor.papel()).isEqualTo("GESTOR_RAIZ");
    }

    @Test
    void cargoFinalRejeitaConfiguracaoDeEscaladaMasAceitaConsultaECapacidadeOrdinaria() {
        CUsuario raiz = criarRaiz();
        autenticar(raiz);

        assertBadRequest(() -> rbacService.cadastrar(requisicaoCargo(
                "FINAL_LIBERAR", true, EComportamentoPadraoPermissao.liberar, List.of(), List.of())));
        assertBadRequest(() -> rbacService.cadastrar(requisicaoCargo(
                "FINAL_GLOBAL", true, EComportamentoPadraoPermissao.bloquear, List.of(),
                List.of(new RFuncionalidadeCargoRbac("gerenciarRegistros", true)))));
        assertBadRequest(() -> rbacService.cadastrar(requisicaoCargo(
                "FINAL_WRITE", true, EComportamentoPadraoPermissao.bloquear,
                List.of(new RPermissaoCargoRbac("api", "POST /usuarios", true)), List.of())));

        RCargoRbac finalLegitimo = rbacService.cadastrar(requisicaoCargo(
                "FINAL_LEGITIMO", true, EComportamentoPadraoPermissao.bloquear,
                List.of(new RPermissaoCargoRbac("api", "POST /usuarios/consulta", true)),
                List.of(new RFuncionalidadeCargoRbac("exportarDados", true))));

        assertThat(finalLegitimo.permissoes()).containsExactly(new RPermissaoCargoRbac("api", "POST /usuarios/consulta", true));
        assertThat(finalLegitimo.funcionalidades()).containsExactly(new RFuncionalidadeCargoRbac("exportarDados", true));
    }

    @Test
    void campoDeNivelAusenteCriaFinalEPreservaNivelPersistidoNoUpdate() {
        CUsuario raiz = criarRaiz();
        autenticar(raiz);
        RCargoRbac gestor = rbacService.cadastrar(requisicaoCargo(
                "GESTOR_LEGADO", false, EComportamentoPadraoPermissao.bloquear, List.of(), List.of()));

        RCargoRbac atualizado = rbacService.atualizar(gestor.id(), new RCargoRbac(
                gestor.id(), gestor.papel(), gestor.nome(), gestor.icone(), gestor.descricao(),
                EComportamentoPadraoPermissao.bloquear, List.of(), List.of(),
                new RRedirecionamentoInicialRbac("/", null, List.of()), true, null, null));
        RCargoRbac criadoLegado = rbacService.cadastrar(requisicaoCargo("FINAL_LEGADO"));

        assertThat(atualizado.destinadoClienteFinal()).isFalse();
        assertThat(criadoLegado.destinadoClienteFinal()).isTrue();
    }

    @Test
    void atorInativoOuPrincipalForjadoNaoEscalaPeloCargoDoPrincipal() {
        criarRaiz();
        CUsuario gestor = usuarioService.criarUsuarioSistema(
                "Gestor inativo", "gestor-inativo-" + SEQUENCIA.incrementAndGet() + "@example.com", "senha-segura", "ADMIN", false);
        autenticar(gestor);
        assertNegado(() -> rbacService.cadastrar(requisicaoCargo("INATIVO")));

        CUsuario finalPersistido = usuarioService.criarUsuarioSistema(
                "Final", "final-forjado-" + SEQUENCIA.incrementAndGet() + "@example.com", "senha-segura", "USER", true);
        CUsuario principalForjado = new CUsuario();
        principalForjado.setIdUsuario(finalPersistido.getIdUsuario());
        principalForjado.setAtivo(true);
        principalForjado.setCargo(cargoRepository.findByPapel("ADMIN").orElseThrow());
        autenticar(principalForjado);
        assertNegado(() -> rbacService.cadastrar(requisicaoCargo("FORJADO")));
    }

    private void autenticar(CUsuario pUsuario) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        pUsuario, pUsuario.getPassword(), pUsuario.getAuthorities()));
    }

    private CUsuario criarRaiz() {
        CUsuario existente = usuarioRepository.findById(1L).orElse(null);
        if (existente != null) {
            return existente;
        }
        CCargoRbac cargoAdmin = cargoRepository.findByPapel("ADMIN").orElseThrow();
        entityManager.createNativeQuery("""
                        INSERT INTO usuarios (id_usuario, nome, email, senha, notificar, ativo, id_cargo, criado_em)
                        VALUES (1, 'Raiz', :email, 'senha-codificada', false, true, :idCargo, CURRENT_TIMESTAMP)
                        """)
                .setParameter("email", "raiz-" + SEQUENCIA.incrementAndGet() + "@example.com")
                .setParameter("idCargo", cargoAdmin.getIdCargo())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        return usuarioRepository.findById(1L).orElseThrow();
    }

    private void liberarGerenciamentoGlobal(CCargoRbac pCargo) {
        CFuncionalidadeCargoRbac funcionalidade = new CFuncionalidadeCargoRbac();
        funcionalidade.setFuncionalidade("gerenciarRegistros");
        funcionalidade.setLiberado(true);
        pCargo.definirFuncionalidades(List.of(funcionalidade));
        cargoRepository.saveAndFlush(pCargo);
    }

    private RUsuario requisicaoUsuario(String pNome, String pPapel) {
        return new RUsuario(
                null, pNome, pNome + "-" + SEQUENCIA.incrementAndGet() + "@example.com", null, null, false, true, pPapel, null);
    }

    private RCargoRbac requisicaoCargo(String pPapel) {
        return requisicaoCargo(pPapel, null, EComportamentoPadraoPermissao.bloquear, List.of(), List.of());
    }

    private RCargoRbac requisicaoCargo(
            String pPapel,
            Boolean pDestinadoClienteFinal,
            EComportamentoPadraoPermissao pComportamento,
            List<RPermissaoCargoRbac> pPermissoes,
            List<RFuncionalidadeCargoRbac> pFuncionalidades) {
        return new RCargoRbac(
                null,
                pPapel,
                "Cargo " + pPapel,
                "mdi-account",
                "Cargo de teste",
                pComportamento,
                pPermissoes,
                pFuncionalidades,
                new RRedirecionamentoInicialRbac("/", null, List.of()),
                true,
                pDestinadoClienteFinal,
                null);
    }

    private void assertBadRequest(Runnable pOperacao) {
        assertThatThrownBy(pOperacao::run)
                .isInstanceOfSatisfying(CExceptionsSystem.class,
                        pErro -> assertThat(pErro.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private void assertNegado(Runnable pOperacao) {
        assertThatThrownBy(pOperacao::run)
                .isInstanceOfSatisfying(CExceptionsSystem.class,
                        pErro -> {
                            org.assertj.core.api.Assertions.assertThat(pErro.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                            org.assertj.core.api.Assertions.assertThat(pErro.getMessage()).isEqualTo("Operação não autorizada");
                        });
    }
}
