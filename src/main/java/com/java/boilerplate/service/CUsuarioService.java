package com.java.boilerplate.service;

import com.java.boilerplate.dto.usuarios.RUsuario;
import com.java.boilerplate.dto.consulta.RConsultaRegistros;
import com.java.boilerplate.dto.consulta.RRespostaConsultaRegistros;
import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.model.CCargoRbac;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.IUsuarioRepository;
import com.java.boilerplate.service.base.CBaseConsultaService;
import com.java.boilerplate.service.base.IServiceCrud;
import com.java.boilerplate.config.security.CTokenService;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
public class CUsuarioService extends CBaseConsultaService<CUsuario, RUsuario> implements IServiceCrud<RUsuario> {
    private static final Long ID_USUARIO_RAIZ = 1L;

    private final IUsuarioRepository usuarioRepository;
    private final CRbacService rbacService;
    private final PasswordEncoder passwordEncoder;
    private final CAuditoriaRegistroService auditoriaRegistroService;
    private final CTokenService tokenService;

    public CUsuarioService(
            EntityManager pEntityManager,
            IUsuarioRepository pUsuarioRepository,
            CRbacService pRbacService,
            PasswordEncoder pPasswordEncoder,
            CAuditoriaRegistroService pAuditoriaRegistroService,
            CTokenService pTokenService
    ) {
        super(pEntityManager, CUsuario.class);
        this.usuarioRepository = pUsuarioRepository;
        this.rbacService = pRbacService;
        this.passwordEncoder = pPasswordEncoder;
        this.auditoriaRegistroService = pAuditoriaRegistroService;
        this.tokenService = pTokenService;
    }

    @Transactional(readOnly = true)
    public CUsuario buscarEntidadePorId(Long pIdUsuario) {
        return usuarioRepository.findById(pIdUsuario)
                .orElseThrow(() -> new CExceptionsSystem("Usuário não encontrado para o ID: " + pIdUsuario, HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public CUsuario buscarEntidadePorEmail(String pEmail) {
        return usuarioRepository.findByEmailIgnoreCase(pEmail)
                .orElseThrow(() -> new CExceptionsSystem("Usuário não encontrado", HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public RUsuario buscarPorId(Long pIdUsuario) {
        return paraRegistro(buscarEntidadePorId(pIdUsuario));
    }

    @Transactional(readOnly = true)
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public RRespostaConsultaRegistros<RUsuario> consultar(RConsultaRegistros pConsulta) {
        return super.consultar(pConsulta);
    }

    @Transactional
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public RUsuario cadastrar(RUsuario pRequest) {
        validarEmailDisponivel(pRequest.email(), null);

        CUsuario usuario = new CUsuario();
        preencherUsuario(usuario, pRequest);
        usuario.setSenha(passwordEncoder.encode(UUID.randomUUID().toString()));
        return paraRegistro(usuarioRepository.saveAndFlush(usuario));
    }

    @Transactional
    public CUsuario criarUsuarioSistema(String pNome, String pEmail, String pSenha, String pPapel, Boolean pAtivo) {
        if (usuarioRepository.existsByEmailIgnoreCase(pEmail)) {
            return usuarioRepository.findByEmailIgnoreCase(pEmail).orElseThrow();
        }

        CUsuario usuario = new CUsuario();
        usuario.setNome(pNome);
        usuario.setEmail(pEmail.toLowerCase());
        usuario.setSenha(passwordEncoder.encode(pSenha));
        usuario.setCargo(rbacService.buscarEntidadePorPapel(pPapel));
        usuario.setAtivo(pAtivo);
        usuario.setNotificar(false);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public CUsuario criarUsuarioSolicitacaoAcesso(String pNome, String pEmail, String pSenha) {
        validarEmailDisponivel(pEmail, null);

        CUsuario usuario = new CUsuario();
        usuario.setNome(pNome);
        usuario.setEmail(pEmail.toLowerCase());
        usuario.setSenha(passwordEncoder.encode(pSenha));
        usuario.setCargo(rbacService.buscarEntidadePorPapel("USER"));
        usuario.setAtivo(false);
        usuario.setNotificar(false);
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public CUsuario vincularIdentidadeGoogle(CUsuario pUsuario, String pGoogleSubject) {
        if (pUsuario == null || pGoogleSubject == null || pGoogleSubject.isBlank()) {
            throw new CExceptionsSystem("Identidade do Google inválida", HttpStatus.UNAUTHORIZED);
        }
        if (pUsuario.getGoogleSubject() != null && !pUsuario.getGoogleSubject().equals(pGoogleSubject)) {
            throw new CExceptionsSystem("Identidade do Google inválida", HttpStatus.UNAUTHORIZED);
        }
        usuarioRepository.findByGoogleSubject(pGoogleSubject)
                .filter(pVinculado -> !pVinculado.getIdUsuario().equals(pUsuario.getIdUsuario()))
                .ifPresent(pVinculado -> {
                    throw new CExceptionsSystem("Identidade do Google inválida", HttpStatus.UNAUTHORIZED);
                });
        pUsuario.setGoogleSubject(pGoogleSubject);
        return usuarioRepository.saveAndFlush(pUsuario);
    }

    @Transactional
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public RUsuario editar(RUsuario pRequest) {
        if (pRequest.id() == null) {
            throw new CExceptionsSystem("O identificador do usuário é obrigatório para edição", HttpStatus.BAD_REQUEST);
        }

        return atualizar(pRequest.id(), pRequest);
    }

    @Transactional
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public RUsuario modificar(RUsuario pRequest) {
        return editar(pRequest);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public RUsuario atualizar(Long pIdUsuario, RUsuario pRequest) {
        CUsuario usuario = buscarEntidadePorId(pIdUsuario);
        validarEmailDisponivel(pRequest.email(), pIdUsuario);
        preencherUsuario(usuario, pRequest);
        CUsuario atualizado = usuarioRepository.saveAndFlush(usuario);
        tokenService.revogarSessoesUsuario(pIdUsuario);
        return paraRegistro(atualizado);
    }

    @Transactional
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void excluir(Long pIdUsuario) {
        if (ID_USUARIO_RAIZ.equals(pIdUsuario)) {
            throw new CExceptionsSystem("O usuário raiz da aplicação não pode ser removido", HttpStatus.BAD_REQUEST);
        }

        Long idUsuarioLogado = resolverIdUsuarioLogado();
        if (pIdUsuario != null && pIdUsuario.equals(idUsuarioLogado)) {
            throw new CExceptionsSystem("O usuário autenticado não pode remover a própria conta", HttpStatus.BAD_REQUEST);
        }

        CUsuario usuario = buscarEntidadePorId(pIdUsuario);
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
        tokenService.revogarSessoesUsuario(pIdUsuario);
    }

    private void preencherUsuario(CUsuario pUsuario, RUsuario pRequest) {
        CCargoRbac cargo = rbacService.buscarEntidadePorPapel(pRequest.papel());
        if (!Boolean.TRUE.equals(cargo.getAtivo())) {
            throw new CExceptionsSystem("O cargo informado está inativo", HttpStatus.BAD_REQUEST);
        }

        pUsuario.setNome(pRequest.nome());
        pUsuario.setEmail(pRequest.email().toLowerCase());
        pUsuario.setAvatar(pRequest.avatar());
        pUsuario.setTelefone(pRequest.telefone());
        pUsuario.setNotificar(Boolean.TRUE.equals(pRequest.notificar()));
        pUsuario.setAtivo(pRequest.ativo() == null || pRequest.ativo());
        pUsuario.setCargo(cargo);
    }

    @Override
    protected Set<String> camposFiltroPermitidos() {
        return Set.of("id", "nome", "email", "telefone", "ativo", "papel");
    }

    @Override
    protected String campoCursor() {
        return "idUsuario";
    }

    @Override
    protected RUsuario paraRegistro(CUsuario pUsuario) {
        return RUsuario.fromEntity(pUsuario, auditoriaRegistroService.montar(pUsuario));
    }

    @Override
    protected Long extrairProximaEntrada(CUsuario pUsuario) {
        return pUsuario.getIdUsuario();
    }

    @Override
    protected String mapearCampoFiltro(String pCampo) {
        return switch (pCampo) {
            case "id" -> "idUsuario";
            case "papel" -> "cargo.papel";
            default -> pCampo;
        };
    }

    @Override
    protected org.springframework.data.jpa.domain.Specification<CUsuario> criarSpecificationBase() {
        return (pRoot, pQuery, pCriteriaBuilder) ->
                pCriteriaBuilder.greaterThan(pRoot.get("idUsuario").as(Long.class), ID_USUARIO_RAIZ);
    }

    private void validarEmailDisponivel(String pEmail, Long pIdUsuarioIgnorado) {
        usuarioRepository.findByEmailIgnoreCase(pEmail)
                .filter(pUsuario -> pIdUsuarioIgnorado == null || !pUsuario.getIdUsuario().equals(pIdUsuarioIgnorado))
                .ifPresent(pUsuario -> {
                    throw new CExceptionsSystem("Já existe um usuário cadastrado com esse e-mail", HttpStatus.CONFLICT);
                });
    }

    private Long resolverIdUsuarioLogado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CUsuario usuario) {
            return usuario.getIdUsuario();
        }

        return null;
    }
}
