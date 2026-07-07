package com.java.boilerplate.service;

import com.java.boilerplate.dto.common.RRespostaPaginacao;
import com.java.boilerplate.dto.filtros.RParametrosPaginacao;
import com.java.boilerplate.dto.usuarios.RUsuario;
import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.model.CCargoRbac;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.IUsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CUsuarioService {
    private final IUsuarioRepository usuarioRepository;
    private final CRbacService rbacService;
    private final PasswordEncoder passwordEncoder;

    public CUsuarioService(IUsuarioRepository pUsuarioRepository, CRbacService pRbacService, PasswordEncoder pPasswordEncoder) {
        this.usuarioRepository = pUsuarioRepository;
        this.rbacService = pRbacService;
        this.passwordEncoder = pPasswordEncoder;
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
    public RRespostaPaginacao<RUsuario> consultar(RParametrosPaginacao pParametros) {
        RRespostaPaginacao<CUsuario> pagina = usuarioRepository.consultarPaginado(pParametros, "idUsuario");
        return new RRespostaPaginacao<>(
                pagina.limite(),
                pagina.proximaEntrada(),
                pagina.items().stream().map(RUsuario::fromEntity).toList(),
                pagina.temMaisRegistros()
        );
    }

    @Transactional(readOnly = true)
    public RUsuario buscarPorId(Long pIdUsuario) {
        return RUsuario.fromEntity(buscarEntidadePorId(pIdUsuario));
    }

    @Transactional
    public RUsuario criar(RUsuario pRequest) {
        validarEmailDisponivel(pRequest.email(), null);

        CUsuario usuario = new CUsuario();
        preencherUsuario(usuario, pRequest);
        usuario.setSenha(passwordEncoder.encode(UUID.randomUUID().toString()));
        return RUsuario.fromEntity(usuarioRepository.save(usuario));
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
    public RUsuario atualizar(Long pIdUsuario, RUsuario pRequest) {
        CUsuario usuario = buscarEntidadePorId(pIdUsuario);
        validarEmailDisponivel(pRequest.email(), pIdUsuario);
        preencherUsuario(usuario, pRequest);
        return RUsuario.fromEntity(usuarioRepository.save(usuario));
    }

    @Transactional
    public void excluir(Long pIdUsuario) {
        CUsuario usuario = buscarEntidadePorId(pIdUsuario);
        usuario.setAtivo(false);
        usuarioRepository.save(usuario);
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

    private void validarEmailDisponivel(String pEmail, Long pIdUsuarioIgnorado) {
        usuarioRepository.findByEmailIgnoreCase(pEmail)
                .filter(pUsuario -> pIdUsuarioIgnorado == null || !pUsuario.getIdUsuario().equals(pIdUsuarioIgnorado))
                .ifPresent(pUsuario -> {
                    throw new CExceptionsSystem("Já existe um usuário cadastrado com esse e-mail", HttpStatus.CONFLICT);
                });
    }
}
