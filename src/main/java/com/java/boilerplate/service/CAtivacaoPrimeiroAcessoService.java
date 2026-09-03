package com.java.boilerplate.service;

import com.java.boilerplate.config.RAtivacaoProperties;
import com.java.boilerplate.config.security.CTokenService;
import com.java.boilerplate.dto.auth.RPrimeiroAcessoSenha;
import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.model.CUsuarioAtivacao;
import com.java.boilerplate.repository.IUsuarioAtivacaoRepository;
import com.java.boilerplate.service.helpers.CAtivacaoTokenService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CAtivacaoPrimeiroAcessoService {
    private static final String MENSAGEM_LINK_INVALIDO = "Link de ativação inválido ou expirado";
    private final IUsuarioAtivacaoRepository ativacaoRepository;
    private final CAtivacaoTokenService ativacaoTokenService;
    private final RAtivacaoProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final CTokenService tokenService;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    public CAtivacaoPrimeiroAcessoService(
            IUsuarioAtivacaoRepository pAtivacaoRepository,
            CAtivacaoTokenService pAtivacaoTokenService,
            RAtivacaoProperties pProperties,
            PasswordEncoder pPasswordEncoder,
            CTokenService pTokenService,
            ApplicationEventPublisher pEventPublisher,
            EntityManager pEntityManager
    ) {
        this.ativacaoRepository = pAtivacaoRepository;
        this.ativacaoTokenService = pAtivacaoTokenService;
        this.properties = pProperties;
        this.passwordEncoder = pPasswordEncoder;
        this.tokenService = pTokenService;
        this.eventPublisher = pEventPublisher;
        this.entityManager = pEntityManager;
    }

    @Transactional
    public void emitir(CUsuario pUsuario) {
        CUsuarioAtivacao ativacao = ativacaoRepository.findByUsuarioIdForUpdate(pUsuario.getIdUsuario()).orElse(null);
        CUsuario usuario = ativacao == null ? pUsuario : ativacao.getUsuario();
        entityManager.refresh(usuario, LockModeType.PESSIMISTIC_WRITE);
        if (Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new CExceptionsSystem("A conta já está ativa", HttpStatus.BAD_REQUEST);
        }
        String token = ativacaoTokenService.gerarToken();
        if (ativacao == null) {
            ativacao = new CUsuarioAtivacao();
        }
        ativacao.setUsuario(usuario);
        ativacao.setTokenHash(ativacaoTokenService.gerarHash(token));
        ativacao.setExpiraEm(LocalDateTime.now().plusMinutes(properties.expirationMinutes()));
        ativacao.setUtilizado(false);
        ativacao.setUtilizadoEm(null);
        ativacaoRepository.save(ativacao);
        eventPublisher.publishEvent(new RAtivacaoEmailEvent(
                usuario.getEmail(), usuario.getNome(), token, properties.expirationMinutes()));
    }

    @Transactional
    public Optional<CUsuario> bloquearUsuarioDaAtivacao(Long pIdUsuario) {
        return ativacaoRepository.findByUsuarioIdForUpdate(pIdUsuario)
                .map(pAtivacao -> {
                    CUsuario usuario = pAtivacao.getUsuario();
                    entityManager.refresh(usuario, LockModeType.PESSIMISTIC_WRITE);
                    return usuario;
                });
    }

    @Transactional
    public void consumir(RPrimeiroAcessoSenha pRequest) {
        validarSenha(pRequest);
        CUsuarioAtivacao ativacao = ativacaoRepository.findByTokenHashForUpdate(ativacaoTokenService.gerarHash(pRequest.token()))
                .orElseThrow(this::linkInvalido);
        CUsuario usuario = ativacao.getUsuario();
        entityManager.refresh(usuario, LockModeType.PESSIMISTIC_WRITE);
        if (Boolean.TRUE.equals(ativacao.getUtilizado())
                || !ativacao.getExpiraEm().isAfter(LocalDateTime.now())
                || Boolean.TRUE.equals(usuario.getAtivo())) {
            throw linkInvalido();
        }
        usuario.setSenha(passwordEncoder.encode(pRequest.senha()));
        usuario.setAtivo(true);
        ativacao.setUtilizado(true);
        ativacao.setUtilizadoEm(LocalDateTime.now());
        tokenService.revogarSessoesUsuario(usuario.getIdUsuario());
    }

    public String hashParaLimite(String pToken) {
        return ativacaoTokenService.gerarHash(pToken);
    }

    private void validarSenha(RPrimeiroAcessoSenha pRequest) {
        if (!pRequest.senha().equals(pRequest.confirmarSenha())) {
            throw new CExceptionsSystem("A confirmação da senha não confere", HttpStatus.BAD_REQUEST);
        }
        if (pRequest.senha().length() < 8 || pRequest.senha().length() > 72) {
            throw new CExceptionsSystem("A senha deve ter entre 8 e 72 caracteres", HttpStatus.BAD_REQUEST);
        }
    }

    private CExceptionsSystem linkInvalido() {
        return new CExceptionsSystem(MENSAGEM_LINK_INVALIDO, HttpStatus.UNAUTHORIZED);
    }
}
