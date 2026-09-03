package com.java.boilerplate.service;

import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.model.CEntidadeAuditavel;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.IUsuarioRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CAutorizacaoAutoriaService {
    public static final String MENSAGEM_ACESSO_NEGADO = "Operação não autorizada";
    private static final String FUNCIONALIDADE_GERENCIAR_REGISTROS = "gerenciarRegistros";

    private final IUsuarioRepository usuarioRepository;

    public CAutorizacaoAutoriaService(IUsuarioRepository pUsuarioRepository) {
        this.usuarioRepository = pUsuarioRepository;
    }

    @Transactional(readOnly = true)
    public <T extends CEntidadeAuditavel> T autorizarGerenciamento(
            Optional<T> pRegistro,
            Supplier<? extends RuntimeException> pExcecaoRegistroAusente) {
        Long idUsuarioAutenticado = resolverIdUsuarioAutenticado();
        boolean registroProprio = pRegistro
                .map(CEntidadeAuditavel::getCriadoPor)
                .filter(pCriadoPor -> Objects.equals(pCriadoPor, idUsuarioAutenticado))
                .isPresent();

        if (!registroProprio && !possuiPermissaoGlobal(idUsuarioAutenticado)) {
            negarAcesso();
        }

        return pRegistro.orElseThrow(pExcecaoRegistroAusente);
    }

    public Long resolverIdUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw acessoNegado();
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CUsuario usuario) || usuario.getIdUsuario() == null) {
            throw acessoNegado();
        }

        return usuario.getIdUsuario();
    }

    private boolean possuiPermissaoGlobal(Long pIdUsuario) {
        return usuarioRepository.possuiFuncionalidadeLiberada(
                pIdUsuario, FUNCIONALIDADE_GERENCIAR_REGISTROS);
    }

    private void negarAcesso() {
        throw acessoNegado();
    }

    private CExceptionsSystem acessoNegado() {
        return new CExceptionsSystem(MENSAGEM_ACESSO_NEGADO, HttpStatus.FORBIDDEN);
    }
}
