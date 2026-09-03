package com.java.boilerplate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.java.boilerplate.dto.common.RAuditoriaRegistro;
import com.java.boilerplate.dto.rbac.RCargoRbac;
import com.java.boilerplate.dto.rbac.RFuncionalidadeCargoRbac;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class CAutorizacaoAutoriaIntegrationTests {
    private static final String FUNCIONALIDADE_GLOBAL = "gerenciarRegistros";
    private static final AtomicLong SEQUENCIA = new AtomicLong();

    @Autowired
    private CUsuarioService usuarioService;

    @Autowired
    private CRbacService rbacService;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private ICargoRbacRepository cargoRepository;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void autorPodeAtualizarEExcluirUsuariosQueCriouSemFuncionalidadeGlobal() {
        CUsuario autor = criarAtor(EComportamentoPadraoPermissao.bloquear, null, null);
        CUsuario usuarioAtualizado = criarUsuarioCriadoPor(autor, autor.getCargo());
        CUsuario usuarioExcluido = criarUsuarioCriadoPor(autor, autor.getCargo());
        autenticar(autor);

        RUsuario resposta = usuarioService.atualizar(
                usuarioAtualizado.getIdUsuario(), requisicaoUsuario(usuarioAtualizado, "Nome atualizado", null));
        usuarioService.excluir(usuarioExcluido.getIdUsuario());

        assertThat(resposta.nome()).isEqualTo("Nome atualizado");
        assertThat(usuarioRepository.findById(usuarioExcluido.getIdUsuario()).orElseThrow().getAtivo()).isFalse();
    }

    @Test
    void autorPodeAtualizarEExcluirCargosQueCriouSemFuncionalidadeGlobal() {
        CUsuario autor = criarAtor(EComportamentoPadraoPermissao.bloquear, null, null);
        CCargoRbac cargoAtualizado = criarCargoCriadoPor(autor, "OWNER_UPDATE");
        CCargoRbac cargoExcluido = criarCargoCriadoPor(autor, "OWNER_DELETE");
        autenticar(autor);

        RCargoRbac resposta = rbacService.atualizar(
                cargoAtualizado.getIdCargo(), requisicaoCargo(cargoAtualizado, "Cargo atualizado", null));
        rbacService.excluir(cargoExcluido.getIdCargo());

        assertThat(resposta.nome()).isEqualTo("Cargo atualizado");
        assertThat(cargoRepository.findById(cargoExcluido.getIdCargo())).isEmpty();
    }

    @Test
    void terceiroSemFuncionalidadeGlobalNaoPodeAtualizarNemExcluirUsuarios() {
        CUsuario ator = criarAtor(EComportamentoPadraoPermissao.bloquear, null, null);
        CUsuario outroAutor = criarAtor(EComportamentoPadraoPermissao.bloquear, null, null);
        CUsuario usuarioAtualizado = criarUsuarioCriadoPor(outroAutor, ator.getCargo());
        CUsuario usuarioExcluido = criarUsuarioCriadoPor(outroAutor, ator.getCargo());
        autenticar(ator);

        assertNegado(() -> usuarioService.atualizar(
                usuarioAtualizado.getIdUsuario(), requisicaoUsuario(usuarioAtualizado, "Ataque", null)));
        assertNegado(() -> usuarioService.excluir(usuarioExcluido.getIdUsuario()));

        assertThat(usuarioRepository.findById(usuarioAtualizado.getIdUsuario()).orElseThrow().getNome())
                .isNotEqualTo("Ataque");
        assertThat(usuarioRepository.findById(usuarioExcluido.getIdUsuario()).orElseThrow().getAtivo()).isTrue();
    }

    @Test
    void terceiroSemFuncionalidadeGlobalNaoPodeAtualizarNemExcluirCargos() {
        CUsuario ator = criarAtor(EComportamentoPadraoPermissao.bloquear, null, null);
        CUsuario outroAutor = criarAtor(EComportamentoPadraoPermissao.bloquear, null, null);
        CCargoRbac cargoAtualizado = criarCargoCriadoPor(outroAutor, "THIRD_UPDATE");
        CCargoRbac cargoExcluido = criarCargoCriadoPor(outroAutor, "THIRD_DELETE");
        autenticar(ator);

        assertNegado(() -> rbacService.atualizar(
                cargoAtualizado.getIdCargo(), requisicaoCargo(cargoAtualizado, "Ataque", null)));
        assertNegado(() -> rbacService.excluir(cargoExcluido.getIdCargo()));

        assertThat(cargoRepository.findById(cargoAtualizado.getIdCargo()).orElseThrow().getNome())
                .isNotEqualTo("Ataque");
        assertThat(cargoRepository.findById(cargoExcluido.getIdCargo())).isPresent();
    }

    @Test
    void funcionalidadeGlobalExplicitamenteLiberadaPermiteGerenciarTerceiros() {
        CUsuario gestor = criarAtor(EComportamentoPadraoPermissao.bloquear, FUNCIONALIDADE_GLOBAL, true);
        CUsuario outroAutor = criarAtor(EComportamentoPadraoPermissao.bloquear, null, null);
        CUsuario usuarioAtualizado = criarUsuarioCriadoPor(outroAutor, gestor.getCargo());
        CUsuario usuarioExcluido = criarUsuarioCriadoPor(outroAutor, gestor.getCargo());
        CCargoRbac cargoAtualizado = criarCargoCriadoPor(outroAutor, "GLOBAL_UPDATE");
        CCargoRbac cargoExcluido = criarCargoCriadoPor(outroAutor, "GLOBAL_DELETE");
        autenticar(gestor);

        usuarioService.atualizar(
                usuarioAtualizado.getIdUsuario(), requisicaoUsuario(usuarioAtualizado, "Gestao global", null));
        usuarioService.excluir(usuarioExcluido.getIdUsuario());
        rbacService.atualizar(
                cargoAtualizado.getIdCargo(), requisicaoCargo(cargoAtualizado, "Gestao global", null));
        rbacService.excluir(cargoExcluido.getIdCargo());

        assertThat(usuarioRepository.findById(usuarioAtualizado.getIdUsuario()).orElseThrow().getNome())
                .isEqualTo("Gestao global");
        assertThat(usuarioRepository.findById(usuarioExcluido.getIdUsuario()).orElseThrow().getAtivo()).isFalse();
        assertThat(cargoRepository.findById(cargoAtualizado.getIdCargo()).orElseThrow().getNome())
                .isEqualTo("Gestao global");
        assertThat(cargoRepository.findById(cargoExcluido.getIdCargo())).isEmpty();
    }

    @Test
    void decisaoGlobalConsultaBancoEIgnoraColecaoDesatualizadaDoPrincipal() {
        CUsuario gestorPersistido = criarAtor(
                EComportamentoPadraoPermissao.bloquear, FUNCIONALIDADE_GLOBAL, true);
        CUsuario outroAutor = criarAtor(EComportamentoPadraoPermissao.bloquear, null, null);
        CUsuario usuarioPermitido = criarUsuarioCriadoPor(outroAutor, gestorPersistido.getCargo());
        CUsuario principalSemFuncionalidades = copiarPrincipalComFuncionalidades(gestorPersistido, List.of());
        autenticar(principalSemFuncionalidades);

        usuarioService.atualizar(
                usuarioPermitido.getIdUsuario(), requisicaoUsuario(usuarioPermitido, "Permitido pelo banco", null));

        assertThat(usuarioRepository.findById(usuarioPermitido.getIdUsuario()).orElseThrow().getNome())
                .isEqualTo("Permitido pelo banco");

        CUsuario atorBloqueadoPersistido = criarAtor(
                EComportamentoPadraoPermissao.bloquear, FUNCIONALIDADE_GLOBAL, false);
        CUsuario usuarioNegado = criarUsuarioCriadoPor(outroAutor, atorBloqueadoPersistido.getCargo());
        CFuncionalidadeCargoRbac funcionalidadeDesatualizada = new CFuncionalidadeCargoRbac();
        funcionalidadeDesatualizada.setFuncionalidade(FUNCIONALIDADE_GLOBAL);
        funcionalidadeDesatualizada.setLiberado(true);
        CUsuario principalComLiberacaoDesatualizada = copiarPrincipalComFuncionalidades(
                atorBloqueadoPersistido, List.of(funcionalidadeDesatualizada));
        autenticar(principalComLiberacaoDesatualizada);

        assertNegado(() -> usuarioService.excluir(usuarioNegado.getIdUsuario()));
    }

    @Test
    void funcionalidadeGlobalExplicitamenteBloqueadaNaoPermiteGerenciarTerceiros() {
        CUsuario ator = criarAtor(EComportamentoPadraoPermissao.bloquear, FUNCIONALIDADE_GLOBAL, false);
        CUsuario outroAutor = criarAtor(EComportamentoPadraoPermissao.bloquear, null, null);
        CUsuario usuario = criarUsuarioCriadoPor(outroAutor, ator.getCargo());
        CCargoRbac cargo = criarCargoCriadoPor(outroAutor, "EXPLICIT_FALSE");
        autenticar(ator);

        assertNegado(() -> usuarioService.atualizar(
                usuario.getIdUsuario(), requisicaoUsuario(usuario, "Ataque", null)));
        assertNegado(() -> usuarioService.excluir(usuario.getIdUsuario()));
        assertNegado(() -> rbacService.atualizar(cargo.getIdCargo(), requisicaoCargo(cargo, "Ataque", null)));
        assertNegado(() -> rbacService.excluir(cargo.getIdCargo()));
    }

    @Test
    void comportamentoPadraoLiberarNaoSubstituiFuncionalidadeGlobalExplicita() {
        CUsuario ator = criarAtor(EComportamentoPadraoPermissao.liberar, null, null);
        CUsuario outroAutor = criarAtor(EComportamentoPadraoPermissao.bloquear, null, null);
        CUsuario usuario = criarUsuarioCriadoPor(outroAutor, ator.getCargo());
        CCargoRbac cargo = criarCargoCriadoPor(outroAutor, "DEFAULT_ALLOW");
        autenticar(ator);

        assertNegado(() -> usuarioService.atualizar(
                usuario.getIdUsuario(), requisicaoUsuario(usuario, "Ataque", null)));
        assertNegado(() -> usuarioService.excluir(usuario.getIdUsuario()));
        assertNegado(() -> rbacService.atualizar(cargo.getIdCargo(), requisicaoCargo(cargo, "Ataque", null)));
        assertNegado(() -> rbacService.excluir(cargo.getIdCargo()));
    }

    @Test
    void chaveLegadaNaoSubstituiFuncionalidadeGlobalExata() {
        CUsuario ator = criarAtor(EComportamentoPadraoPermissao.bloquear, "gerenciarRegistrosOutros", true);
        CUsuario outroAutor = criarAtor(EComportamentoPadraoPermissao.bloquear, null, null);
        CUsuario usuario = criarUsuarioCriadoPor(outroAutor, ator.getCargo());
        CCargoRbac cargo = criarCargoCriadoPor(outroAutor, "LEGACY");
        autenticar(ator);

        assertNegado(() -> usuarioService.atualizar(
                usuario.getIdUsuario(), requisicaoUsuario(usuario, "Ataque", null)));
        assertNegado(() -> rbacService.excluir(cargo.getIdCargo()));
    }

    @Test
    void alvoInexistenteSemPermissaoEhIndistinguivelDeTerceiro() {
        CUsuario ator = criarAtor(EComportamentoPadraoPermissao.bloquear, null, null);
        CUsuario outroAutor = criarAtor(EComportamentoPadraoPermissao.bloquear, null, null);
        CUsuario usuarioTerceiro = criarUsuarioCriadoPor(outroAutor, ator.getCargo());
        CCargoRbac cargoTerceiro = criarCargoCriadoPor(outroAutor, "INDISTINGUIVEL");
        autenticar(ator);

        CExceptionsSystem usuarioExistente = capturarNegado(() -> usuarioService.atualizar(
                usuarioTerceiro.getIdUsuario(), requisicaoUsuario(usuarioTerceiro, "Ataque", null)));
        CExceptionsSystem usuarioInexistente = capturarNegado(() -> usuarioService.atualizar(
                Long.MAX_VALUE, requisicaoUsuario(usuarioTerceiro, "Ataque", null)));
        CExceptionsSystem cargoExistente = capturarNegado(() -> rbacService.excluir(cargoTerceiro.getIdCargo()));
        CExceptionsSystem cargoInexistente = capturarNegado(() -> rbacService.excluir(Long.MAX_VALUE));

        assertThat(usuarioInexistente.getMessage()).isEqualTo(usuarioExistente.getMessage());
        assertThat(usuarioInexistente.getStatus()).isEqualTo(usuarioExistente.getStatus());
        assertThat(cargoInexistente.getMessage()).isEqualTo(cargoExistente.getMessage());
        assertThat(cargoInexistente.getStatus()).isEqualTo(cargoExistente.getStatus());
    }

    @Test
    void gestorGlobalRecebeNotFoundParaAlvoInexistente() {
        CUsuario gestor = criarAtor(EComportamentoPadraoPermissao.bloquear, FUNCIONALIDADE_GLOBAL, true);
        autenticar(gestor);

        assertThatThrownBy(() -> usuarioService.excluir(Long.MAX_VALUE))
                .isInstanceOfSatisfying(CExceptionsSystem.class,
                        pErro -> assertThat(pErro.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> rbacService.excluir(Long.MAX_VALUE))
                .isInstanceOfSatisfying(CExceptionsSystem.class,
                        pErro -> assertThat(pErro.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void principalAusenteOuInesperadoFalhaFechadoParaUsuariosECargos() {
        CUsuario outroAutor = criarAtor(EComportamentoPadraoPermissao.bloquear, null, null);
        CUsuario usuario = criarUsuarioCriadoPor(outroAutor, outroAutor.getCargo());
        CCargoRbac cargo = criarCargoCriadoPor(outroAutor, "PRINCIPAL");

        SecurityContextHolder.clearContext();
        assertNegado(() -> usuarioService.atualizar(
                usuario.getIdUsuario(), requisicaoUsuario(usuario, "Ataque", null)));
        assertNegado(() -> rbacService.excluir(cargo.getIdCargo()));

        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("principal-invalido", "senha", List.of()));
        assertNegado(() -> usuarioService.excluir(usuario.getIdUsuario()));
        assertNegado(() -> rbacService.atualizar(cargo.getIdCargo(), requisicaoCargo(cargo, "Ataque", null)));
    }

    @Test
    void auditoriaForjadaNoDtoNaoAlteraDecisao() {
        CUsuario ator = criarAtor(EComportamentoPadraoPermissao.bloquear, null, null);
        CUsuario outroAutor = criarAtor(EComportamentoPadraoPermissao.bloquear, null, null);
        CUsuario usuario = criarUsuarioCriadoPor(outroAutor, ator.getCargo());
        CCargoRbac cargo = criarCargoCriadoPor(outroAutor, "FORGED_AUDIT");
        RAuditoriaRegistro auditoriaForjada = new RAuditoriaRegistro(
                LocalDateTime.now(), ator.getIdUsuario(), ator.getEmail(), null, null, null);
        autenticar(ator);

        assertNegado(() -> usuarioService.atualizar(
                usuario.getIdUsuario(), requisicaoUsuario(usuario, "Ataque", auditoriaForjada)));
        assertNegado(() -> rbacService.atualizar(
                cargo.getIdCargo(), requisicaoCargo(cargo, "Ataque", auditoriaForjada)));
    }

    @Test
    void protecoesDeRootAutoexclusaoECargosPadraoSaoPreservadas() {
        CUsuario candidatoGestor = criarAtor(EComportamentoPadraoPermissao.bloquear, FUNCIONALIDADE_GLOBAL, true);
        criarOuBuscarUsuarioRaiz(candidatoGestor.getCargo());
        CUsuario gestor = usuarioRepository.findById(1L).orElseThrow();
        autenticar(gestor);

        assertBadRequest(() -> usuarioService.excluir(1L));
        assertBadRequest(() -> usuarioService.excluir(gestor.getIdUsuario()));
        assertBadRequest(() -> rbacService.excluir(cargoRepository.findByPapel("ADMIN").orElseThrow().getIdCargo()));
        assertBadRequest(() -> rbacService.excluir(cargoRepository.findByPapel("USER").orElseThrow().getIdCargo()));
    }

    @Test
    void putNaoPodeDesativarUsuarioRaiz() {
        CUsuario candidatoGestor = criarAtor(EComportamentoPadraoPermissao.bloquear, FUNCIONALIDADE_GLOBAL, true);
        CUsuario usuarioRaiz = criarOuBuscarUsuarioRaiz(candidatoGestor.getCargo());
        CUsuario gestor = usuarioRepository.findById(1L).orElseThrow();
        autenticar(gestor);

        assertBadRequest(() -> usuarioService.editar(requisicaoUsuarioComAtivo(
                usuarioRaiz, "Raiz alterado", false)));

        assertThat(usuarioRepository.findById(1L).orElseThrow().getAtivo()).isTrue();
    }

    @Test
    void patchNaoPodeDesativarProprioAtorGlobal() {
        CUsuario candidatoGestor = criarAtor(EComportamentoPadraoPermissao.bloquear, FUNCIONALIDADE_GLOBAL, true);
        criarOuBuscarUsuarioRaiz(candidatoGestor.getCargo());
        CUsuario gestor = usuarioRepository.findById(1L).orElseThrow();
        autenticar(gestor);

        assertBadRequest(() -> usuarioService.modificar(requisicaoUsuarioComAtivo(
                gestor, "Gestor alterado", false)));

        assertThat(usuarioRepository.findById(gestor.getIdUsuario()).orElseThrow().getAtivo()).isTrue();
    }

    @Test
    void atorGlobalPodeAtualizarProprioNomeSemSeDesativar() {
        criarAtor(EComportamentoPadraoPermissao.bloquear, FUNCIONALIDADE_GLOBAL, true);
        CUsuario gestor = usuarioRepository.findById(1L).orElseThrow();
        autenticar(gestor);

        RUsuario resposta = usuarioService.modificar(requisicaoUsuarioComAtivo(
                gestor, "Gestor atualizado", true));

        assertThat(resposta.nome()).isEqualTo("Gestor atualizado");
        assertThat(resposta.ativo()).isTrue();
    }

    @Test
    void cargoAdminNaoPodeSerRenomeadoAntesDaExclusao() {
        criarAtor(EComportamentoPadraoPermissao.bloquear, FUNCIONALIDADE_GLOBAL, true);
        CUsuario gestor = usuarioRepository.findById(1L).orElseThrow();
        CCargoRbac cargoAdmin = cargoRepository.findByPapel("ADMIN").orElseThrow();
        autenticar(gestor);

        assertBadRequest(() -> rbacService.editar(requisicaoCargoComPapel(
                cargoAdmin, "ADMIN_RENOMEADO")));

        assertThat(cargoRepository.findById(cargoAdmin.getIdCargo()).orElseThrow().getPapel()).isEqualTo("ADMIN");
    }

    @Test
    void cargoUserNaoPodeSerRenomeadoAntesDaExclusao() {
        criarAtor(EComportamentoPadraoPermissao.bloquear, FUNCIONALIDADE_GLOBAL, true);
        CUsuario gestor = usuarioRepository.findById(1L).orElseThrow();
        CCargoRbac cargoUser = cargoRepository.findByPapel("USER").orElseThrow();
        autenticar(gestor);

        assertBadRequest(() -> rbacService.modificar(requisicaoCargoComPapel(
                cargoUser, "USER_RENOMEADO")));

        assertThat(cargoRepository.findById(cargoUser.getIdCargo()).orElseThrow().getPapel()).isEqualTo("USER");
    }

    @Test
    void cargoCustomizadoPodeSerRenomeadoEExcluido() {
        criarAtor(EComportamentoPadraoPermissao.bloquear, FUNCIONALIDADE_GLOBAL, true);
        CUsuario gestor = usuarioRepository.findById(1L).orElseThrow();
        CCargoRbac cargo = criarCargoCriadoPor(gestor, "CUSTOM_RENAME");
        autenticar(gestor);

        RCargoRbac atualizado = rbacService.editar(requisicaoCargoComPapel(cargo, "CUSTOM_RENAMED"));
        rbacService.excluir(cargo.getIdCargo());

        assertThat(atualizado.papel()).isEqualTo("CUSTOM_RENAMED");
        assertThat(cargoRepository.findById(cargo.getIdCargo())).isEmpty();
    }

    @Test
    void deleteDeRootSemPermissaoEhIndistinguivelDeTerceiroEInexistente() {
        CUsuario ator = criarAtor(EComportamentoPadraoPermissao.bloquear, null, null);
        CUsuario outroAutor = criarAtor(EComportamentoPadraoPermissao.bloquear, null, null);
        CUsuario usuarioTerceiro = criarUsuarioCriadoPor(outroAutor, ator.getCargo());
        autenticar(ator);

        CExceptionsSystem root = capturarNegado(() -> usuarioService.excluir(1L));
        CExceptionsSystem terceiro = capturarNegado(() -> usuarioService.excluir(usuarioTerceiro.getIdUsuario()));
        CExceptionsSystem inexistente = capturarNegado(() -> usuarioService.excluir(Long.MAX_VALUE));

        assertThat(root.getMessage()).isEqualTo(terceiro.getMessage()).isEqualTo(inexistente.getMessage());
        assertThat(root.getStatus()).isEqualTo(terceiro.getStatus()).isEqualTo(inexistente.getStatus());
    }

    @Test
    void errosDeDominioPermanecemDepoisDaAutorizacaoNosFluxosPutEPatch() {
        criarAtor(EComportamentoPadraoPermissao.bloquear, FUNCIONALIDADE_GLOBAL, true);
        CUsuario gestor = usuarioRepository.findById(1L).orElseThrow();
        CUsuario outroAutor = criarAtor(EComportamentoPadraoPermissao.bloquear, null, null);
        CUsuario usuario = criarUsuarioCriadoPor(outroAutor, gestor.getCargo());
        CUsuario emailConflitante = criarUsuarioCriadoPor(outroAutor, gestor.getCargo());
        CCargoRbac cargo = criarCargoCriadoPor(outroAutor, "DOMAIN_ERRORS");
        CCargoRbac cargoConflitante = criarCargoCriadoPor(outroAutor, "ROLE_CONFLICT");
        CCargoRbac cargoInativo = criarCargo(
                "INACTIVE_" + SEQUENCIA.incrementAndGet(),
                EComportamentoPadraoPermissao.bloquear,
                null,
                null);
        cargoInativo.setAtivo(false);
        cargoRepository.saveAndFlush(cargoInativo);
        autenticar(gestor);

        assertStatus(
                () -> usuarioService.editar(new RUsuario(
                        usuario.getIdUsuario(),
                        usuario.getNome(),
                        emailConflitante.getEmail(),
                        usuario.getAvatar(),
                        usuario.getTelefone(),
                        usuario.getNotificar(),
                        usuario.getAtivo(),
                        usuario.getPapel(),
                        null)),
                HttpStatus.CONFLICT);
        assertStatus(
                () -> usuarioService.modificar(new RUsuario(
                        usuario.getIdUsuario(),
                        usuario.getNome(),
                        usuario.getEmail(),
                        usuario.getAvatar(),
                        usuario.getTelefone(),
                        usuario.getNotificar(),
                        usuario.getAtivo(),
                        cargoInativo.getPapel(),
                        null)),
                HttpStatus.BAD_REQUEST);
        assertStatus(
                () -> rbacService.editar(new RCargoRbac(
                        cargo.getIdCargo(),
                        cargoConflitante.getPapel(),
                        cargo.getNome(),
                        cargo.getIcone(),
                        cargo.getDescricao(),
                        cargo.getComportamentoPadrao(),
                        List.of(),
                        List.of(),
                        new RRedirecionamentoInicialRbac("/", null, List.of()),
                        true,
                        null)),
                HttpStatus.CONFLICT);

        CCargoRbac cargoAdmin = cargoRepository.findByPapel("ADMIN").orElseThrow();
        RCargoRbac adminAtualizado = rbacService.modificar(
                requisicaoCargo(cargoAdmin, "Administrador atualizado", null));
        assertThat(adminAtualizado.nome()).isEqualTo("Administrador atualizado");
    }

    @Test
    void cadastroDeUsuarioContinuaExigindoAdmin() throws Exception {
        PreAuthorize preAuthorize = CUsuarioService.class.getMethod("cadastrar", RUsuario.class)
                .getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasRole('ADMIN')");
    }

    private CUsuario criarAtor(
            EComportamentoPadraoPermissao pComportamento,
            String pFuncionalidade,
            Boolean pLiberado) {
        SecurityContextHolder.clearContext();
        garantirUsuarioRaizPadrao();
        CCargoRbac cargo = criarCargo("ATOR_" + SEQUENCIA.incrementAndGet(), pComportamento, pFuncionalidade, pLiberado);
        return criarUsuario(cargo);
    }

    private void garantirUsuarioRaizPadrao() {
        if (usuarioRepository.findById(1L).isPresent()) {
            return;
        }
        entityManager.createNativeQuery("""
                        INSERT INTO usuarios (
                            id_usuario, nome, email, senha, notificar, ativo, id_cargo, criado_em
                        ) VALUES (
                            1, 'Usuario raiz', 'root-test@example.com', 'senha-codificada', false, true, :idCargo, CURRENT_TIMESTAMP
                        )
                        """)
                .setParameter("idCargo", cargoRepository.findByPapel("ADMIN").orElseThrow().getIdCargo())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    private CCargoRbac criarCargo(
            String pPapel,
            EComportamentoPadraoPermissao pComportamento,
            String pFuncionalidade,
            Boolean pLiberado) {
        CCargoRbac cargo = new CCargoRbac();
        cargo.setPapel(pPapel);
        cargo.setNome("Cargo " + pPapel);
        cargo.setIcone("mdi-account");
        cargo.setDescricao("Cargo de teste");
        cargo.setComportamentoPadrao(pComportamento);
        cargo.setRedirecionamentoPath("/");
        cargo.setRedirecionamentoFiltros("[]");
        cargo.setAtivo(true);
        cargo.setDestinadoClienteFinal(false);
        if (pFuncionalidade != null) {
            CFuncionalidadeCargoRbac funcionalidade = new CFuncionalidadeCargoRbac();
            funcionalidade.setFuncionalidade(pFuncionalidade);
            funcionalidade.setLiberado(pLiberado);
            cargo.definirFuncionalidades(List.of(funcionalidade));
        }
        return cargoRepository.saveAndFlush(cargo);
    }

    private CUsuario criarUsuario(CCargoRbac pCargo) {
        long sequencia = SEQUENCIA.incrementAndGet();
        CUsuario usuario = new CUsuario();
        usuario.setNome("Usuario " + sequencia);
        usuario.setEmail("usuario-" + sequencia + "@example.com");
        usuario.setSenha("senha-codificada");
        usuario.setCargo(pCargo);
        usuario.setAtivo(true);
        usuario.setNotificar(false);
        return usuarioRepository.saveAndFlush(usuario);
    }

    private CUsuario criarOuBuscarUsuarioRaiz(CCargoRbac pCargo) {
        CUsuario existente = usuarioRepository.findById(1L).orElse(null);
        if (existente != null) {
            return existente;
        }
        entityManager.createNativeQuery("""
                        INSERT INTO usuarios (
                            id_usuario, nome, email, senha, notificar, ativo, id_cargo, criado_em
                        ) VALUES (
                            1, 'Usuario raiz', 'root-test@example.com', 'senha-codificada', false, true, :idCargo, CURRENT_TIMESTAMP
                        )
                        """)
                .setParameter("idCargo", pCargo.getIdCargo())
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();
        return usuarioRepository.findById(1L).orElseThrow();
    }

    private CUsuario criarUsuarioCriadoPor(CUsuario pAutor, CCargoRbac pCargo) {
        autenticar(pAutor);
        CCargoRbac cargoUsuario = Boolean.TRUE.equals(pCargo.getDestinadoClienteFinal())
                ? pCargo
                : cargoRepository.findByPapel("USER").orElseThrow();
        CUsuario usuario = criarUsuario(cargoUsuario);
        assertThat(usuario.getCriadoPor()).isEqualTo(pAutor.getIdUsuario());
        return usuario;
    }

    private CCargoRbac criarCargoCriadoPor(CUsuario pAutor, String pPrefixoPapel) {
        autenticar(pAutor);
        CCargoRbac cargo = criarCargo(
                pPrefixoPapel + "_" + SEQUENCIA.incrementAndGet(),
                EComportamentoPadraoPermissao.bloquear,
                null,
                null);
        cargo.setDestinadoClienteFinal(true);
        cargo = cargoRepository.saveAndFlush(cargo);
        assertThat(cargo.getCriadoPor()).isEqualTo(pAutor.getIdUsuario());
        return cargo;
    }

    private void autenticar(CUsuario pUsuario) {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(
                        pUsuario, pUsuario.getPassword(), pUsuario.getAuthorities()));
    }

    private CUsuario copiarPrincipalComFuncionalidades(
            CUsuario pUsuarioPersistido, List<CFuncionalidadeCargoRbac> pFuncionalidades) {
        CCargoRbac cargoDesatualizado = new CCargoRbac();
        cargoDesatualizado.setIdCargo(pUsuarioPersistido.getCargo().getIdCargo());
        cargoDesatualizado.setPapel(pUsuarioPersistido.getCargo().getPapel());
        cargoDesatualizado.setAtivo(true);
        cargoDesatualizado.definirFuncionalidades(pFuncionalidades);
        CUsuario principal = new CUsuario();
        principal.setIdUsuario(pUsuarioPersistido.getIdUsuario());
        principal.setEmail(pUsuarioPersistido.getEmail());
        principal.setSenha(pUsuarioPersistido.getSenha());
        principal.setAtivo(true);
        principal.setCargo(cargoDesatualizado);
        return principal;
    }

    private RUsuario requisicaoUsuario(
            CUsuario pUsuario, String pNome, RAuditoriaRegistro pAuditoria) {
        return new RUsuario(
                pUsuario.getIdUsuario(),
                pNome,
                pUsuario.getEmail(),
                pUsuario.getAvatar(),
                pUsuario.getTelefone(),
                pUsuario.getNotificar(),
                pUsuario.getAtivo(),
                pUsuario.getPapel(),
                pAuditoria);
    }

    private RUsuario requisicaoUsuarioComAtivo(CUsuario pUsuario, String pNome, Boolean pAtivo) {
        return new RUsuario(
                pUsuario.getIdUsuario(),
                pNome,
                pUsuario.getEmail(),
                pUsuario.getAvatar(),
                pUsuario.getTelefone(),
                pUsuario.getNotificar(),
                pAtivo,
                pUsuario.getPapel(),
                null);
    }

    private RCargoRbac requisicaoCargo(
            CCargoRbac pCargo, String pNome, RAuditoriaRegistro pAuditoria) {
        return new RCargoRbac(
                pCargo.getIdCargo(),
                pCargo.getPapel(),
                pNome,
                pCargo.getIcone(),
                pCargo.getDescricao(),
                pCargo.getComportamentoPadrao(),
                List.of(),
                List.of(),
                new RRedirecionamentoInicialRbac("/", null, List.of()),
                pCargo.getAtivo(),
                pAuditoria);
    }

    private RCargoRbac requisicaoCargoComPapel(CCargoRbac pCargo, String pPapel) {
        return new RCargoRbac(
                pCargo.getIdCargo(),
                pPapel,
                pCargo.getNome(),
                pCargo.getIcone(),
                pCargo.getDescricao(),
                pCargo.getComportamentoPadrao(),
                pCargo.getPermissoes().stream().map(com.java.boilerplate.dto.rbac.RPermissaoCargoRbac::fromEntity).toList(),
                pCargo.getFuncionalidades().stream().map(RFuncionalidadeCargoRbac::fromEntity).toList(),
                new RRedirecionamentoInicialRbac("/", null, List.of()),
                pCargo.getAtivo(),
                null);
    }

    private void assertNegado(Runnable pOperacao) {
        assertThatThrownBy(pOperacao::run)
                .isInstanceOfSatisfying(CExceptionsSystem.class, pErro -> {
                    assertThat(pErro.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(pErro.getMessage()).isEqualTo("Operação não autorizada");
                    assertThat(pErro.getCodigo()).isNull();
                    assertThat(pErro.getDados()).isNull();
                });
    }

    private CExceptionsSystem capturarNegado(Runnable pOperacao) {
        try {
            pOperacao.run();
        } catch (CExceptionsSystem pErro) {
            assertThat(pErro.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            return pErro;
        }
        throw new AssertionError("A operação deveria ter sido negada");
    }

    private void assertBadRequest(Runnable pOperacao) {
        assertStatus(pOperacao, HttpStatus.BAD_REQUEST);
    }

    private void assertStatus(Runnable pOperacao, HttpStatus pStatus) {
        assertThatThrownBy(pOperacao::run)
                .isInstanceOfSatisfying(CExceptionsSystem.class,
                        pErro -> assertThat(pErro.getStatus()).isEqualTo(pStatus));
    }
}
