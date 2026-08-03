package com.java.boilerplate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.java.boilerplate.dto.common.RRespostaPaginacao;
import com.java.boilerplate.dto.filtros.RFiltroConsulta;
import com.java.boilerplate.dto.filtros.RParametrosPaginacao;
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
import com.java.boilerplate.repository.IPermissaoCargoRbacRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.AntPathMatcher;

@Service
public class CRbacService {
    private static final String RECURSO_API = "api";
    private static final String RECURSO_ROTAS = "rotas";

    private final ICargoRbacRepository cargoRepository;
    private final IPermissaoCargoRbacRepository permissaoCargoRepository;
    private final CAuditoriaRegistroService auditoriaRegistroService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public CRbacService(
            ICargoRbacRepository pCargoRepository,
            IPermissaoCargoRbacRepository pPermissaoCargoRepository,
            CAuditoriaRegistroService pAuditoriaRegistroService) {
        this.cargoRepository = pCargoRepository;
        this.permissaoCargoRepository = pPermissaoCargoRepository;
        this.auditoriaRegistroService = pAuditoriaRegistroService;
    }

    @Transactional(readOnly = true)
    public CCargoRbac buscarEntidadePorPapel(String pPapel) {
        return cargoRepository
                .findByPapel(pPapel)
                .orElseThrow(() ->
                        new CExceptionsSystem("Cargo não encontrado para o papel: " + pPapel, HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public CCargoRbac buscarEntidadePorId(Long pIdCargo) {
        return cargoRepository
                .findById(pIdCargo)
                .orElseThrow(() ->
                        new CExceptionsSystem("Cargo não encontrado para o ID: " + pIdCargo, HttpStatus.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public RRespostaPaginacao<RCargoRbac> consultar(RParametrosPaginacao pParametros) {
        RRespostaPaginacao<CCargoRbac> pagina = cargoRepository.consultarPaginado(pParametros, "idCargo");
        return new RRespostaPaginacao<>(
                pagina.limite(),
                pagina.proximaEntrada(),
                pagina.items().stream().map(this::toDTO).toList(),
                pagina.temMaisRegistros());
    }

    @Transactional(readOnly = true)
    public List<RCargoRbac> listarTodos() {
        return cargoRepository.findAll().stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public RCargoRbac buscarPorId(Long pIdCargo) {
        return toDTO(buscarEntidadePorId(pIdCargo));
    }

    @Transactional
    public RCargoRbac salvar(RCargoRbac pRequest) {
        validarDelegacaoDentroDoCargoAtual(pRequest);
        if (pRequest.id() == null && cargoRepository.existsByPapel(pRequest.papel())) {
            throw new CExceptionsSystem("Já existe um cargo com o papel informado", HttpStatus.CONFLICT);
        }

        CCargoRbac cargo = pRequest.id() == null ? new CCargoRbac() : buscarEntidadePorId(pRequest.id());
        preencherCargo(cargo, pRequest);
        return toDTO(cargoRepository.save(cargo));
    }

    @Transactional
    public RCargoRbac atualizar(Long pIdCargo, RCargoRbac pRequest) {
        validarDelegacaoDentroDoCargoAtual(pRequest);
        CCargoRbac cargo = buscarEntidadePorId(pIdCargo);
        cargoRepository
                .findByPapel(pRequest.papel())
                .filter(pCargo -> !pCargo.getIdCargo().equals(pIdCargo))
                .ifPresent(pCargo -> {
                    throw new CExceptionsSystem("Já existe um cargo com o papel informado", HttpStatus.CONFLICT);
                });

        preencherCargo(cargo, pRequest);
        return toDTO(cargoRepository.save(cargo));
    }

    @Transactional
    public void excluir(Long pIdCargo) {
        CCargoRbac cargo = buscarEntidadePorId(pIdCargo);
        if ("ADMIN".equals(cargo.getPapel()) || "USER".equals(cargo.getPapel())) {
            throw new CExceptionsSystem(
                    "Os cargos padrão ADMIN e USER não podem ser excluídos", HttpStatus.BAD_REQUEST);
        }
        cargoRepository.delete(cargo);
    }

    @Transactional(readOnly = true)
    public boolean usuarioPodeAcessarEndpoint(CUsuario pUsuario, String pMetodoHttp, String pCaminho) {
        if (pUsuario == null
                || pUsuario.getCargo() == null
                || !Boolean.TRUE.equals(pUsuario.getCargo().getAtivo())) {
            return false;
        }

        CCargoRbac cargo = buscarEntidadePorId(pUsuario.getCargo().getIdCargo());
        List<CPermissaoCargoRbac> permissoesApi =
                permissaoCargoRepository.findByCargo_IdCargoAndRecurso(cargo.getIdCargo(), RECURSO_API);
        Boolean decisaoExplicita = resolverPermissaoExplicita(permissoesApi, pMetodoHttp, pCaminho);

        if (decisaoExplicita != null) {
            return decisaoExplicita;
        }

        return cargo.getComportamentoPadrao() == EComportamentoPadraoPermissao.liberar;
    }

    public RCargoRbac toDTO(CCargoRbac pCargo) {
        return new RCargoRbac(
                pCargo.getIdCargo(),
                pCargo.getPapel(),
                pCargo.getNome(),
                pCargo.getIcone(),
                pCargo.getDescricao(),
                pCargo.getComportamentoPadrao(),
                pCargo.getPermissoes().stream()
                        .map(RPermissaoCargoRbac::fromEntity)
                        .toList(),
                pCargo.getFuncionalidades().stream()
                        .map(RFuncionalidadeCargoRbac::fromEntity)
                        .toList(),
                new RRedirecionamentoInicialRbac(
                        pCargo.getRedirecionamentoPath(),
                        pCargo.getRedirecionamentoName(),
                        lerFiltros(pCargo.getRedirecionamentoFiltros())),
                pCargo.getAtivo(),
                auditoriaRegistroService.montar(pCargo));
    }

    private void preencherCargo(CCargoRbac pCargo, RCargoRbac pRequest) {
        pCargo.setPapel(normalizarPapel(pRequest.papel()));
        pCargo.setNome(pRequest.nome());
        pCargo.setIcone(pRequest.icone());
        pCargo.setDescricao(pRequest.descricao());
        pCargo.setComportamentoPadrao(
                pRequest.comportamentoPadrao() == null
                        ? EComportamentoPadraoPermissao.bloquear
                        : pRequest.comportamentoPadrao());
        pCargo.setAtivo(pRequest.ativo() == null || pRequest.ativo());

        RRedirecionamentoInicialRbac redirecionamento = pRequest.redirecionamentoInicial();
        pCargo.setRedirecionamentoPath(
                redirecionamento == null
                                || redirecionamento.path() == null
                                || redirecionamento.path().isBlank()
                        ? "/"
                        : redirecionamento.path());
        pCargo.setRedirecionamentoName(redirecionamento == null ? null : redirecionamento.name());
        pCargo.setRedirecionamentoFiltros(
                escreverFiltros(redirecionamento == null ? List.of() : redirecionamento.filtros()));

        pCargo.definirPermissoes(normalizarPermissoes(pRequest.permissoes()));
        pCargo.definirFuncionalidades(normalizarFuncionalidades(pRequest.funcionalidades()));
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
                    new RPermissaoCargoRbac(recurso, acao, Boolean.TRUE.equals(permissao.liberado())));
        }

        return permissoesPorChave.values().stream()
                .map(RPermissaoCargoRbac::toEntity)
                .toList();
    }

    private List<CFuncionalidadeCargoRbac> normalizarFuncionalidades(List<RFuncionalidadeCargoRbac> pFuncionalidades) {
        if (pFuncionalidades == null) {
            return new ArrayList<>();
        }

        Map<String, RFuncionalidadeCargoRbac> itens = new LinkedHashMap<>();
        for (RFuncionalidadeCargoRbac funcionalidade : pFuncionalidades) {
            if (funcionalidade == null || funcionalidade.funcionalidade() == null) {
                continue;
            }
            String chave = funcionalidade.funcionalidade().trim();
            if (!chave.isBlank()) {
                itens.put(chave, new RFuncionalidadeCargoRbac(chave, Boolean.TRUE.equals(funcionalidade.liberado())));
            }
        }
        return itens.values().stream().map(RFuncionalidadeCargoRbac::toEntity).toList();
    }

    private void validarDelegacaoDentroDoCargoAtual(RCargoRbac pRequest) {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof CUsuario usuarioAtual) || usuarioAtual.getCargo() == null) {
            return;
        }

        CCargoRbac cargoAtual = buscarEntidadePorId(usuarioAtual.getCargo().getIdCargo());
        if ("ADMIN".equals(cargoAtual.getPapel())) {
            return;
        }

        if (pRequest.comportamentoPadrao() == EComportamentoPadraoPermissao.liberar) {
            throw new CExceptionsSystem(
                    "Cargos delegados devem bloquear acessos não configurados por padrão", HttpStatus.FORBIDDEN);
        }

        RRedirecionamentoInicialRbac redirecionamento = pRequest.redirecionamentoInicial();
        if (redirecionamento != null
                && redirecionamento.name() != null
                && !redirecionamento.name().isBlank()
                && !permissaoEstaLiberada(cargoAtual, RECURSO_ROTAS, redirecionamento.name())) {
            throw new CExceptionsSystem("Não é permitido configurar uma rota inicial sem acesso", HttpStatus.FORBIDDEN);
        }

        if (pRequest.permissoes() != null) {
            pRequest.permissoes().stream()
                    .filter(pItem -> pItem != null && Boolean.TRUE.equals(pItem.liberado()))
                    .filter(pItem -> !permissaoEstaLiberada(cargoAtual, pItem.recurso(), pItem.acao()))
                    .findFirst()
                    .ifPresent(pItem -> {
                        throw new CExceptionsSystem(
                                "Não é permitido delegar a permissão " + pItem.recurso() + ": " + pItem.acao(),
                                HttpStatus.FORBIDDEN);
                    });
        }

        if (pRequest.funcionalidades() != null) {
            pRequest.funcionalidades().stream()
                    .filter(pItem -> pItem != null && Boolean.TRUE.equals(pItem.liberado()))
                    .filter(pItem -> !funcionalidadeEstaLiberada(cargoAtual, pItem.funcionalidade()))
                    .findFirst()
                    .ifPresent(pItem -> {
                        throw new CExceptionsSystem(
                                "Não é permitido delegar a funcionalidade " + pItem.funcionalidade(),
                                HttpStatus.FORBIDDEN);
                    });
        }
    }

    private boolean permissaoEstaLiberada(CCargoRbac pCargo, String pRecurso, String pAcao) {
        if (pRecurso == null || pAcao == null) {
            return false;
        }
        return pCargo.getPermissoes().stream()
                .filter(pItem -> pRecurso.trim().equals(pItem.getRecurso())
                        && pAcao.trim().equals(pItem.getAcao()))
                .map(CPermissaoCargoRbac::getLiberado)
                .findFirst()
                .orElse(pCargo.getComportamentoPadrao() == EComportamentoPadraoPermissao.liberar);
    }

    private boolean funcionalidadeEstaLiberada(CCargoRbac pCargo, String pFuncionalidade) {
        if (pFuncionalidade == null) {
            return false;
        }
        return pCargo.getFuncionalidades().stream()
                .filter(pItem -> pFuncionalidade.trim().equals(pItem.getFuncionalidade()))
                .map(CFuncionalidadeCargoRbac::getLiberado)
                .findFirst()
                .orElse(pCargo.getComportamentoPadrao() == EComportamentoPadraoPermissao.liberar);
    }

    private String normalizarPapel(String pPapel) {
        return pPapel == null ? null : pPapel.trim().toUpperCase();
    }

    private Boolean resolverPermissaoExplicita(
            List<CPermissaoCargoRbac> pPermissoes, String pMetodoHttp, String pCaminho) {
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
            throw new CExceptionsSystem(
                    "Não foi possível serializar os filtros do redirecionamento", HttpStatus.BAD_REQUEST);
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
}
