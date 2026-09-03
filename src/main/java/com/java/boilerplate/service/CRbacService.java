package com.java.boilerplate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.java.boilerplate.cache.IRedisCache;
import com.java.boilerplate.dto.filtros.RFiltroConsulta;
import com.java.boilerplate.dto.rbac.RCargoRbac;
import com.java.boilerplate.dto.rbac.RFuncionalidadeCargoRbac;
import com.java.boilerplate.dto.rbac.RPermissaoCargoRbac;
import com.java.boilerplate.dto.rbac.RRedirecionamentoInicialRbac;
import com.java.boilerplate.enums.EComportamentoPadraoPermissao;
import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.model.CCargoRbac;
import com.java.boilerplate.model.CFuncionalidadeCargoRbac;
import com.java.boilerplate.model.CPermissaoCargoRbac;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.ICargoRbacRepository;
import com.java.boilerplate.service.base.CBaseConsultaService;
import com.java.boilerplate.service.base.IServiceCrud;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class CRbacService extends CBaseConsultaService<CCargoRbac, RCargoRbac> implements IServiceCrud<RCargoRbac> {
    private static final String RECURSO_API = "api";
    private static final String FUNCIONALIDADE_GERENCIAR_REGISTROS = "gerenciarRegistros";
    private static final String ALIAS_GERENCIAR_REGISTROS = "gerenciarRegistrosOutros";
    private static final Set<String> CARGOS_PADRAO = Set.of("ADMIN", "USER");

    private final ICargoRbacRepository cargoRepository;
    private final CAuditoriaRegistroService auditoriaRegistroService;
    private final IRedisCache redisCache;
    private final CAutorizacaoAutoriaService autorizacaoAutoriaService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public CRbacService(
            EntityManager pEntityManager,
            ICargoRbacRepository pCargoRepository,
            CAuditoriaRegistroService pAuditoriaRegistroService,
            IRedisCache pRedisCache,
            CAutorizacaoAutoriaService pAutorizacaoAutoriaService
    ) {
        super(pEntityManager, CCargoRbac.class);
        this.cargoRepository = pCargoRepository;
        this.auditoriaRegistroService = pAuditoriaRegistroService;
        this.redisCache = pRedisCache;
        this.autorizacaoAutoriaService = pAutorizacaoAutoriaService;
    }

    @Transactional(readOnly = true)
    public CCargoRbac buscarEntidadePorPapel(String pPapel) {
        return cargoRepository.findByPapel(pPapel)
                .orElseThrow(() -> new CExceptionsSystem("Cargo não encontrado para o papel: " + pPapel, HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public CCargoRbac buscarEntidadePorId(Long pIdCargo) {
        return cargoRepository.findById(pIdCargo)
                .orElseThrow(() -> new CExceptionsSystem("Cargo não encontrado para o ID: " + pIdCargo, HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<RCargoRbac> listarTodos() {
        return cargoRepository.findAllWithPermissoes().stream().map(this::paraRegistro).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public RCargoRbac buscarPorId(Long pIdCargo) {
        return paraRegistro(buscarEntidadePorId(pIdCargo));
    }

    @Transactional
    @Override
    public RCargoRbac cadastrar(RCargoRbac pRequest) {
        if (pRequest.id() != null) {
            throw new CExceptionsSystem("Um novo cargo não pode possuir identificador", HttpStatus.BAD_REQUEST);
        }

        if (cargoRepository.existsByPapel(pRequest.papel())) {
            throw new CExceptionsSystem("Já existe um cargo com o papel informado", HttpStatus.CONFLICT);
        }

        CCargoRbac cargo = new CCargoRbac();
        preencherCargo(cargo, pRequest);
        CCargoRbac salvo = cargoRepository.save(cargo);
        invalidarAposCommit(salvo.getIdCargo());
        return paraRegistro(salvo);
    }

    @Transactional
    @Override
    public RCargoRbac editar(RCargoRbac pRequest) {
        if (pRequest.id() == null) {
            throw new CExceptionsSystem("O identificador do cargo é obrigatório para edição", HttpStatus.BAD_REQUEST);
        }

        return atualizar(pRequest.id(), pRequest);
    }

    @Transactional
    @Override
    public RCargoRbac modificar(RCargoRbac pRequest) {
        return editar(pRequest);
    }

    @Transactional
    public RCargoRbac atualizar(Long pIdCargo, RCargoRbac pRequest) {
        CCargoRbac cargo = autorizacaoAutoriaService.autorizarGerenciamento(
                cargoRepository.findById(pIdCargo),
                () -> new CExceptionsSystem("Cargo não encontrado para o ID: " + pIdCargo, HttpStatus.NOT_FOUND));
        if (CARGOS_PADRAO.contains(cargo.getPapel())
                && !cargo.getPapel().equals(normalizarPapel(pRequest.papel()))) {
            throw new CExceptionsSystem("Os cargos padrão ADMIN e USER não podem ser renomeados", HttpStatus.BAD_REQUEST);
        }
        cargoRepository.findByPapel(pRequest.papel())
                .filter(pCargo -> !pCargo.getIdCargo().equals(pIdCargo))
                .ifPresent(pCargo -> {
                    throw new CExceptionsSystem("Já existe um cargo com o papel informado", HttpStatus.CONFLICT);
                });

        preencherCargo(cargo, pRequest);
        CCargoRbac atualizado = cargoRepository.save(cargo);
        invalidarAposCommit(atualizado.getIdCargo());
        return paraRegistro(atualizado);
    }

    @Transactional
    @Override
    public void excluir(Long pIdCargo) {
        CCargoRbac cargo = autorizacaoAutoriaService.autorizarGerenciamento(
                cargoRepository.findById(pIdCargo),
                () -> new CExceptionsSystem("Cargo não encontrado para o ID: " + pIdCargo, HttpStatus.NOT_FOUND));
        if (CARGOS_PADRAO.contains(cargo.getPapel())) {
            throw new CExceptionsSystem("Os cargos padrão ADMIN e USER não podem ser excluídos", HttpStatus.BAD_REQUEST);
        }
        cargoRepository.delete(cargo);
        invalidarAposCommit(pIdCargo);
    }

    @Transactional(readOnly = true)
    public boolean usuarioPodeAcessarEndpoint(CUsuario pUsuario, String pMetodoHttp, String pCaminho) {
        if (pUsuario == null || pUsuario.getCargo() == null || !Boolean.TRUE.equals(pUsuario.getCargo().getAtivo())) {
            return false;
        }

        RPermissoesCargoCache permissoes = obterPermissoes(pUsuario.getCargo().getIdCargo());
        if (!permissoes.ativo()) {
            return false;
        }
        Boolean decisaoExplicita = resolverPermissaoExplicitaCache(permissoes.permissoes(), pMetodoHttp, pCaminho);

        if (decisaoExplicita != null) {
            return decisaoExplicita;
        }

        return permissoes.comportamentoPadrao() == EComportamentoPadraoPermissao.liberar;
    }

    @Override
    protected Set<String> camposFiltroPermitidos() {
        return Set.of("nome", "descricao", "comportamentoPadrao", "ativo");
    }

    @Override
    protected String campoCursor() {
        return "idCargo";
    }

    @Override
    protected Long extrairProximaEntrada(CCargoRbac pCargo) {
        return pCargo.getIdCargo();
    }

    @Override
    public RCargoRbac paraRegistro(CCargoRbac pCargo) {
        return new RCargoRbac(
                pCargo.getIdCargo(),
                pCargo.getPapel(),
                pCargo.getNome(),
                pCargo.getIcone(),
                pCargo.getDescricao(),
                pCargo.getComportamentoPadrao(),
                pCargo.getPermissoes().stream().map(RPermissaoCargoRbac::fromEntity).toList(),
                pCargo.getFuncionalidades().stream().map(RFuncionalidadeCargoRbac::fromEntity).toList(),
                new RRedirecionamentoInicialRbac(
                        pCargo.getRedirecionamentoPath(),
                        pCargo.getRedirecionamentoName(),
                        lerFiltros(pCargo.getRedirecionamentoFiltros())
                ),
                pCargo.getAtivo(),
                auditoriaRegistroService.montar(pCargo)
        );
    }

    private void preencherCargo(CCargoRbac pCargo, RCargoRbac pRequest) {
        pCargo.setPapel(normalizarPapel(pRequest.papel()));
        pCargo.setNome(pRequest.nome());
        pCargo.setIcone(pRequest.icone());
        pCargo.setDescricao(pRequest.descricao());
        pCargo.setComportamentoPadrao(pRequest.comportamentoPadrao() == null ? EComportamentoPadraoPermissao.bloquear : pRequest.comportamentoPadrao());
        pCargo.setAtivo(pRequest.ativo() == null || pRequest.ativo());

        RRedirecionamentoInicialRbac redirecionamento = pRequest.redirecionamentoInicial();
        pCargo.setRedirecionamentoPath(redirecionamento == null || redirecionamento.path() == null || redirecionamento.path().isBlank() ? "/" : redirecionamento.path());
        pCargo.setRedirecionamentoName(redirecionamento == null ? null : redirecionamento.name());
        pCargo.setRedirecionamentoFiltros(escreverFiltros(redirecionamento == null ? List.of() : redirecionamento.filtros()));

        pCargo.definirPermissoes(normalizarPermissoes(pRequest.permissoes()));
        pCargo.definirFuncionalidades(normalizarFuncionalidades(pRequest.funcionalidades()));
    }

    private List<CFuncionalidadeCargoRbac> normalizarFuncionalidades(
            List<RFuncionalidadeCargoRbac> pFuncionalidades) {
        if (pFuncionalidades == null) {
            return List.of();
        }

        boolean possuiValorCanonicoExplicito = pFuncionalidades.stream()
                .anyMatch(pItem -> FUNCIONALIDADE_GERENCIAR_REGISTROS.equals(
                        normalizarNomeFuncionalidade(pItem.funcionalidade())));

        return pFuncionalidades.stream()
                .filter(pItem -> !possuiValorCanonicoExplicito
                        || !ALIAS_GERENCIAR_REGISTROS.equals(
                                normalizarNomeFuncionalidade(pItem.funcionalidade())))
                .map(pItem -> new RFuncionalidadeCargoRbac(
                        normalizarNomeFuncionalidadePersistida(pItem.funcionalidade()),
                        pItem.liberado()).toEntity())
                .toList();
    }

    private String normalizarNomeFuncionalidadePersistida(String pFuncionalidade) {
        String funcionalidade = normalizarNomeFuncionalidade(pFuncionalidade);
        return ALIAS_GERENCIAR_REGISTROS.equals(funcionalidade)
                ? FUNCIONALIDADE_GERENCIAR_REGISTROS
                : funcionalidade;
    }

    private String normalizarNomeFuncionalidade(String pFuncionalidade) {
        return pFuncionalidade == null ? null : pFuncionalidade.trim();
    }

    private List<CPermissaoCargoRbac> normalizarPermissoes(List<RPermissaoCargoRbac> pPermissoes) {
        if (pPermissoes == null) {
            return new ArrayList<>();
        }

        Map<String, RPermissaoCargoRbac> permissoesPorChave = new LinkedHashMap<>();
        for (RPermissaoCargoRbac permissao : pPermissoes) {
            if (permissao == null || permissao.recurso() == null || permissao.acao() == null) {
                continue;
            }

            String recurso = permissao.recurso().trim();
            String acao = permissao.acao().trim();
            if (recurso.isBlank() || acao.isBlank()) {
                continue;
            }

            permissoesPorChave.put(
                    recurso + "::" + acao,
                    new RPermissaoCargoRbac(recurso, acao, Boolean.TRUE.equals(permissao.liberado()))
            );
        }

        return permissoesPorChave.values().stream().map(RPermissaoCargoRbac::toEntity).toList();
    }

    private String normalizarPapel(String pPapel) {
        return pPapel == null ? null : pPapel.trim().toUpperCase();
    }

    private Boolean resolverPermissaoExplicita(List<CPermissaoCargoRbac> pPermissoes, String pMetodoHttp, String pCaminho) {
        Boolean permissaoLiberada = null;

        for (CPermissaoCargoRbac permissao : pPermissoes) {
            if (!acaoApiCombina(permissao.getAcao(), pMetodoHttp, pCaminho)) {
                continue;
            }

            if (Boolean.FALSE.equals(permissao.getLiberado())) {
                return false;
            }

            permissaoLiberada = true;
        }

        return permissaoLiberada;
    }

    private Boolean resolverPermissaoExplicitaCache(List<RPermissaoApiCache> pPermissoes, String pMetodoHttp, String pCaminho) {
        Boolean permissaoLiberada = null;
        for (RPermissaoApiCache permissao : pPermissoes) {
            if (!acaoApiCombina(permissao.acao(), pMetodoHttp, pCaminho)) {
                continue;
            }
            if (!permissao.liberado()) {
                return false;
            }
            permissaoLiberada = true;
        }
        return permissaoLiberada;
    }

    private RPermissoesCargoCache obterPermissoes(Long pIdCargo) {
        String chaveCache = chaveCache(pIdCargo);
        Optional<RPermissoesCargoCache> permissoesEmCache = redisCache.obter(chaveCache).flatMap(this::desserializarPermissoes);
        if (permissoesEmCache.isPresent()) {
            return permissoesEmCache.get();
        }
        CCargoRbac cargo = cargoRepository.findByIdWithPermissoes(pIdCargo)
                .orElseThrow(() -> new CExceptionsSystem("Cargo não encontrado", HttpStatus.NOT_FOUND));
        RPermissoesCargoCache permissoes = new RPermissoesCargoCache(
                Boolean.TRUE.equals(cargo.getAtivo()),
                cargo.getComportamentoPadrao(),
                cargo.getPermissoes().stream().filter(pPermissao -> RECURSO_API.equals(pPermissao.getRecurso()))
                        .map(pPermissao -> new RPermissaoApiCache(pPermissao.getAcao(), Boolean.TRUE.equals(pPermissao.getLiberado())))
                        .toList());
        try {
            redisCache.salvarPermanente(chaveCache, objectMapper.writeValueAsString(permissoes));
        } catch (JsonProcessingException pException) {
            // O banco relacional continua sendo usado quando a serializacao falha.
        }
        return permissoes;
    }

    private Optional<RPermissoesCargoCache> desserializarPermissoes(String pValor) {
        try {
            return Optional.of(objectMapper.readValue(pValor, RPermissoesCargoCache.class));
        } catch (JsonProcessingException pException) {
            return Optional.empty();
        }
    }

    private void invalidarAposCommit(Long pIdCargo) {
        if (pIdCargo == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            redisCache.remover(chaveCache(pIdCargo));
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisCache.remover(chaveCache(pIdCargo));
            }
        });
    }

    private String chaveCache(Long pIdCargo) {
        return "v1:rbac:cargo:" + pIdCargo;
    }

    private boolean acaoApiCombina(String pAcao, String pMetodoHttp, String pCaminho) {
        if (pAcao == null || !pAcao.contains(" ")) {
            return false;
        }

        String[] partes = pAcao.trim().split("\\s+", 2);
        String metodo = partes[0];
        String padraoCaminho = partes[1];

        return metodo.equalsIgnoreCase(pMetodoHttp) && antPathMatcher.match(padraoCaminho, pCaminho);
    }

    private String escreverFiltros(List<RFiltroConsulta> pFiltros) {
        try {
            return objectMapper.writeValueAsString(pFiltros == null ? List.of() : pFiltros);
        } catch (JsonProcessingException pException) {
            throw new CExceptionsSystem("Não foi possível serializar os filtros do redirecionamento", HttpStatus.BAD_REQUEST);
        }
    }

    private List<RFiltroConsulta> lerFiltros(String pJson) {
        if (pJson == null || pJson.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(pJson, new TypeReference<>() {});
        } catch (JsonProcessingException pException) {
            return List.of();
        }
    }

    private record RPermissaoApiCache(String acao, boolean liberado) {}

    private record RPermissoesCargoCache(boolean ativo, EComportamentoPadraoPermissao comportamentoPadrao, List<RPermissaoApiCache> permissoes) {}
}
