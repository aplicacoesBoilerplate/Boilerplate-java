package com.java.boilerplate.service;

import com.java.boilerplate.dto.preferencias.RPreferenciaUsuario;
import com.java.boilerplate.dto.preferencias.RPreferenciasUsuario;
import com.java.boilerplate.model.CPreferenciaUsuario;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.repository.IPreferenciaUsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;

@Service
public class CPreferenciaUsuarioService {
    private static final int MAXIMO_PREFERENCIAS_POR_USUARIO = 100;
    private static final int MAXIMO_ITENS_POR_LOTE = 20;
    private static final int MAXIMO_CARACTERES_VALOR = 16_384;
    private final IPreferenciaUsuarioRepository preferenciaUsuarioRepository;
    private final CAuthService authService;
    private final EntityManager entityManager;

    public CPreferenciaUsuarioService(
            IPreferenciaUsuarioRepository pPreferenciaUsuarioRepository,
            CAuthService pAuthService,
            EntityManager pEntityManager
    ) {
        this.preferenciaUsuarioRepository = pPreferenciaUsuarioRepository;
        this.authService = pAuthService;
        this.entityManager = pEntityManager;
    }

    @Transactional(readOnly = true)
    public RPreferenciasUsuario buscarPreferenciasUsuarioAutenticado() {
        CUsuario usuario = authService.buscarUsuarioLogado();
        List<RPreferenciaUsuario> preferencias = preferenciaUsuarioRepository
                .findTop100ByUsuario_IdUsuarioOrderByContextoAscChaveAsc(usuario.getIdUsuario())
                .stream()
                .map(RPreferenciaUsuario::fromEntity)
                .toList();

        return new RPreferenciasUsuario(preferencias);
    }

    @Transactional
    public RPreferenciasUsuario salvarPreferenciasUsuarioAutenticado(RPreferenciasUsuario pRequest) {
        CUsuario usuario = authService.buscarUsuarioLogado();

        if (pRequest == null || pRequest.preferencias() == null || pRequest.preferencias().isEmpty()) {
            return buscarPreferenciasUsuarioAutenticado();
        }
        if (pRequest.preferencias().size() > MAXIMO_ITENS_POR_LOTE) {
            throw new CExceptionsSystem("O lote aceita no máximo 20 preferências", HttpStatus.BAD_REQUEST);
        }

        var chavesRecebidas = new HashSet<String>();
        pRequest.preferencias().forEach(pPreferencia -> {
            validarPreferencia(pPreferencia);
            if (!chavesRecebidas.add(chave(pPreferencia.contexto(), pPreferencia.chave()))) {
                throw new CExceptionsSystem("O lote não aceita preferências duplicadas", HttpStatus.BAD_REQUEST);
            }
        });

        entityManager.lock(usuario, LockModeType.PESSIMISTIC_WRITE);
        List<CPreferenciaUsuario> existentes = preferenciaUsuarioRepository
                .findTop100ByUsuario_IdUsuarioOrderByContextoAscChaveAsc(usuario.getIdUsuario());
        Map<String, CPreferenciaUsuario> porChave = new LinkedHashMap<>();
        existentes.forEach(pPreferencia -> porChave.put(chave(pPreferencia.getContexto(), pPreferencia.getChave()), pPreferencia));
        long novas = pRequest.preferencias().stream()
                .filter(pPreferencia -> !porChave.containsKey(chave(pPreferencia.contexto(), pPreferencia.chave())))
                .count();
        if (existentes.size() + novas > MAXIMO_PREFERENCIAS_POR_USUARIO) {
            throw new CExceptionsSystem("Cada usuário pode manter no máximo 100 preferências", HttpStatus.BAD_REQUEST);
        }

        List<CPreferenciaUsuario> atualizadas = pRequest.preferencias().stream().map(pPreferencia -> {
            CPreferenciaUsuario entidade = porChave.getOrDefault(chave(pPreferencia.contexto(), pPreferencia.chave()), new CPreferenciaUsuario());
            entidade.setUsuario(usuario);
            entidade.setContexto(pPreferencia.contexto());
            entidade.setChave(pPreferencia.chave());
            entidade.setValorJson(pPreferencia.valorJson());
            return entidade;
        }).toList();
        preferenciaUsuarioRepository.saveAll(atualizadas);

        return buscarPreferenciasUsuarioAutenticado();
    }

    @Transactional
    public RPreferenciaUsuario salvarPreferenciaUsuarioAutenticado(RPreferenciaUsuario pRequest) {
        CUsuario usuario = authService.buscarUsuarioLogado();
        return RPreferenciaUsuario.fromEntity(salvarPreferencia(usuario, pRequest));
    }

    private CPreferenciaUsuario salvarPreferencia(CUsuario pUsuario, RPreferenciaUsuario pRequest) {
        validarPreferencia(pRequest);
        var preferenciaExistente = preferenciaUsuarioRepository
                .findByUsuario_IdUsuarioAndContextoAndChave(pUsuario.getIdUsuario(), pRequest.contexto(), pRequest.chave());
        if (preferenciaExistente.isEmpty()) {
            // Serializa alteracoes de quota por usuario. A restricao unica protege a
            // chave; este lock protege o limite agregado de registros concorrentes.
            entityManager.lock(pUsuario, LockModeType.PESSIMISTIC_WRITE);
            if (preferenciaUsuarioRepository.countByUsuario_IdUsuario(pUsuario.getIdUsuario()) >= MAXIMO_PREFERENCIAS_POR_USUARIO) {
                throw new CExceptionsSystem("Cada usuário pode manter no máximo 100 preferências", HttpStatus.BAD_REQUEST);
            }
        }
        CPreferenciaUsuario preferencia = preferenciaExistente.orElse(new CPreferenciaUsuario());

        preferencia.setUsuario(pUsuario);
        preferencia.setContexto(pRequest.contexto());
        preferencia.setChave(pRequest.chave());
        preferencia.setValorJson(pRequest.valorJson());
        return preferenciaUsuarioRepository.save(preferencia);
    }

    private String chave(String pContexto, String pChave) {
        return pContexto + "\u0000" + pChave;
    }

    private void validarPreferencia(RPreferenciaUsuario pRequest) {
        if (pRequest == null
                || pRequest.contexto() == null
                || pRequest.contexto().isBlank()
                || pRequest.contexto().length() > 120
                || pRequest.chave() == null
                || pRequest.chave().isBlank()
                || pRequest.chave().length() > 120) {
            throw new CExceptionsSystem("Contexto e chave devem possuir entre 1 e 120 caracteres", HttpStatus.BAD_REQUEST);
        }
        if (pRequest.valorJson() == null || pRequest.valorJson().isBlank()
                || pRequest.valorJson().length() > MAXIMO_CARACTERES_VALOR) {
            throw new CExceptionsSystem("O valor da preferência aceita no máximo 16384 caracteres", HttpStatus.BAD_REQUEST);
        }
    }
}
