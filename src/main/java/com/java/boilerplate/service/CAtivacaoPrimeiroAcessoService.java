package com.java.boilerplate.service;

import com.java.boilerplate.config.RAtivacaoProperties;
import com.java.boilerplate.config.security.CTokenService;
import com.java.boilerplate.dto.auth.RPrimeiroAcessoSenha;
import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.model.CUsuarioAtivacao;
import com.java.boilerplate.repository.IUsuarioAtivacaoRepository;
import com.java.boilerplate.service.helpers.CAtivacaoTokenService;
import java.time.LocalDateTime;
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

    public CAtivacaoPrimeiroAcessoService(
            IUsuarioAtivacaoRepository pAtivacaoRepository,
            CAtivacaoTokenService pAtivacaoTokenService,
            RAtivacaoProperties pProperties,
            PasswordEncoder pPasswordEncoder,
            CTokenService pTokenService,
            ApplicationEventPublisher pEventPublisher
    ) {
        this.ativacaoRepository = pAtivacaoRepository;
        this.ativacaoTokenService = pAtivacaoTokenService;
        this.properties = pProperties;
        this.passwordEncoder = pPasswordEncoder;
        this.tokenService = pTokenService;
        this.eventPublisher = pEventPublisher;
    }

    @Transactional
    public void emitir(CUsuario pUsuario) {
        String token = ativacaoTokenService.gerarToken();
        CUsuarioAtivacao ativacao = ativacaoRepository.findByUsuario_IdUsuario(pUsuario.getIdUsuario())
                .orElseGet(CUsuarioAtivacao::new);
        ativacao.setUsuario(pUsuario);
        ativacao.setTokenHash(ativacaoTokenService.gerarHash(token));
        ativacao.setExpiraEm(LocalDateTime.now().plusMinutes(properties.expirationMinutes()));
        ativacao.setUtilizado(false);
        ativacao.setUtilizadoEm(null);
        ativacaoRepository.save(ativacao);
        eventPublisher.publishEvent(new RAtivacaoEmailEvent(
                pUsuario.getEmail(), pUsuario.getNome(), token, properties.expirationMinutes()));
    }

    @Transactional
    public void consumir(RPrimeiroAcessoSenha pRequest) {
        validarSenha(pRequest);
        CUsuarioAtivacao ativacao = ativacaoRepository.findByTokenHashForUpdate(ativacaoTokenService.gerarHash(pRequest.token()))
                .orElseThrow(this::linkInvalido);
        CUsuario usuario = ativacao.getUsuario();
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
