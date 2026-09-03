package com.java.boilerplate.service;

import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.model.CCargoRbac;
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
    private static final Long ID_USUARIO_RAIZ = 1L;

    private final IUsuarioRepository usuarioRepository;

    public CAutorizacaoAutoriaService(IUsuarioRepository pUsuarioRepository) {
        this.usuarioRepository = pUsuarioRepository;
    }

    @Transactional(readOnly = true)
    public <T extends CEntidadeAuditavel> T autorizarGerenciamento(
            Optional<T> pRegistro,
            Supplier<? extends RuntimeException> pExcecaoRegistroAusente) {
        CUsuario ator = obterAtorAtivo();
        Long idUsuarioAutenticado = ator.getIdUsuario();
        boolean registroProprio = pRegistro
                .map(CEntidadeAuditavel::getCriadoPor)
                .filter(pCriadoPor -> Objects.equals(pCriadoPor, idUsuarioAutenticado))
                .isPresent();

        if (!ehRaiz(ator) && !registroProprio && !possuiPermissaoGlobal(idUsuarioAutenticado)) {
            negarAcesso();
        }

        return pRegistro.orElseThrow(pExcecaoRegistroAusente);
    }

    public Long resolverIdUsuarioAutenticado() {
        return obterAtorAtivo().getIdUsuario();
    }

    @Transactional(readOnly = true)
    public void autorizarCriacaoCargo(boolean pDestinadoClienteFinal) {
        validarOperacaoCargo(null, pDestinadoClienteFinal);
    }

    @Transactional(readOnly = true)
    public void autorizarGerenciamentoCargo(CCargoRbac pCargoAtual, boolean pDestinadoClienteFinal) {
        validarOperacaoCargo(pCargoAtual, pDestinadoClienteFinal);
    }

    @Transactional(readOnly = true)
    public void autorizarGerenciamentoUsuario(CUsuario pUsuarioAtual) {
        CUsuario ator = validarAdministradorGestor();
        if (!ehRaiz(ator) && pUsuarioAtual != null
                && (pUsuarioAtual.getCargo() == null
                || !Boolean.TRUE.equals(pUsuarioAtual.getCargo().getDestinadoClienteFinal()))) {
            negarAcesso();
        }
    }

    @Transactional(readOnly = true)
    public void autorizarAtribuicaoCargo(CCargoRbac pCargoDestino) {
        CUsuario ator = validarAdministradorGestor();
        if (!ehRaiz(ator) && (pCargoDestino == null
                || !Boolean.TRUE.equals(pCargoDestino.getDestinadoClienteFinal()))) {
            negarAcesso();
        }
    }

    private void validarOperacaoCargo(CCargoRbac pCargoAtual, boolean pDestinadoClienteFinal) {
        CUsuario ator = validarAdministradorGestor();
        if (ehRaiz(ator)) {
            return;
        }
        if (!pDestinadoClienteFinal || (pCargoAtual != null
                && !Boolean.TRUE.equals(pCargoAtual.getDestinadoClienteFinal()))) {
            negarAcesso();
        }
    }

    private CUsuario validarAdministradorGestor() {
        CUsuario ator = obterAtorAtivo();
        if (ehRaiz(ator)) {
            return ator;
        }
        CCargoRbac cargo = ator.getCargo();
        if (Boolean.TRUE.equals(cargo.getDestinadoClienteFinal())) {
            negarAcesso();
        }
        return ator;
    }

    private CUsuario obterAtorAtivo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw acessoNegado();
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CUsuario usuario) || usuario.getIdUsuario() == null) {
            throw acessoNegado();
        }

        CUsuario ator = usuarioRepository.findById(usuario.getIdUsuario()).orElseThrow(this::acessoNegado);
        if (!Boolean.TRUE.equals(ator.getAtivo())
                || ator.getCargo() == null
                || !Boolean.TRUE.equals(ator.getCargo().getAtivo())) {
            throw acessoNegado();
        }
        return ator;
    }

    private boolean ehRaiz(CUsuario pAtor) {
        return ID_USUARIO_RAIZ.equals(pAtor.getIdUsuario());
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
