package com.java.boilerplate.service;

import com.java.boilerplate.dto.preferencias.RPreferenciaUsuario;
import com.java.boilerplate.dto.preferencias.RPreferenciasUsuario;
import com.java.boilerplate.model.CPreferenciaUsuario;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.IPreferenciaUsuarioRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CPreferenciaUsuarioService {
    private final IPreferenciaUsuarioRepository preferenciaUsuarioRepository;
    private final CAuthService authService;

    public CPreferenciaUsuarioService(
            IPreferenciaUsuarioRepository pPreferenciaUsuarioRepository, CAuthService pAuthService) {
        this.preferenciaUsuarioRepository = pPreferenciaUsuarioRepository;
        this.authService = pAuthService;
    }

    @Transactional(readOnly = true)
    public RPreferenciasUsuario buscarPreferenciasUsuarioAutenticado() {
        CUsuario usuario = authService.buscarUsuarioLogado();
        List<RPreferenciaUsuario> preferencias =
                preferenciaUsuarioRepository
                        .findByUsuario_IdUsuarioOrderByContextoAscChaveAsc(usuario.getIdUsuario())
                        .stream()
                        .map(RPreferenciaUsuario::fromEntity)
                        .toList();

        return new RPreferenciasUsuario(preferencias);
    }

    @Transactional
    public RPreferenciasUsuario salvarPreferenciasUsuarioAutenticado(RPreferenciasUsuario pRequest) {
        CUsuario usuario = authService.buscarUsuarioLogado();

        if (pRequest.preferencias() != null) {
            pRequest.preferencias().forEach(pPreferencia -> salvarPreferencia(usuario, pPreferencia));
        }

        return buscarPreferenciasUsuarioAutenticado();
    }

    @Transactional
    public RPreferenciaUsuario salvarPreferenciaUsuarioAutenticado(RPreferenciaUsuario pRequest) {
        CUsuario usuario = authService.buscarUsuarioLogado();
        return RPreferenciaUsuario.fromEntity(salvarPreferencia(usuario, pRequest));
    }

    private CPreferenciaUsuario salvarPreferencia(CUsuario pUsuario, RPreferenciaUsuario pRequest) {
        CPreferenciaUsuario preferencia = preferenciaUsuarioRepository
                .findByUsuario_IdUsuarioAndContextoAndChave(
                        pUsuario.getIdUsuario(), pRequest.contexto(), pRequest.chave())
                .orElse(new CPreferenciaUsuario());

        preferencia.setUsuario(pUsuario);
        preferencia.setContexto(pRequest.contexto());
        preferencia.setChave(pRequest.chave());
        preferencia.setValorJson(pRequest.valorJson());
        return preferenciaUsuarioRepository.save(preferencia);
    }
}
