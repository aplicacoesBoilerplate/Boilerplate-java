package com.java.boilerplate.service.base;

import com.java.boilerplate.dto.consulta.RConsultaRegistros;
import com.java.boilerplate.dto.consulta.RRespostaConsultaRegistros;
import com.java.boilerplate.dto.filtros.RFiltroConsulta;
import com.java.boilerplate.enums.EOperadorFiltro;
import com.java.boilerplate.exception.CExceptionsSystem;
import com.java.boilerplate.repository.specifications.CGenericSpecification;
import com.java.boilerplate.repository.specifications.COffsetSpecification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * @description Implementa consulta paginada por cursor sem executar COUNT, preservando o contrato do frontend.
 * @template TEntidade - Entidade JPA consultada.
 * @template TRegistro - DTO retornado ao cliente.
 */
public abstract class CBaseConsultaService<TEntidade, TRegistro> implements IServiceConsulta<TRegistro> {
    private static final int LIMITE_PADRAO = 20;
    private static final int LIMITE_MAXIMO = 100;

    private final EntityManager entityManager;
    private final Class<TEntidade> classeEntidade;

    protected CBaseConsultaService(EntityManager pEntityManager, Class<TEntidade> pClasseEntidade) {
        this.entityManager = pEntityManager;
        this.classeEntidade = pClasseEntidade;
    }

    /**
     * @description Executa uma consulta paginada com limite adicional para identificar a próxima página sem COUNT.
     * @param pConsulta - Parâmetros recebidos do frontend.
     * @returns Página de registros no contrato compartilhado.
     */
    @Override
    @Transactional(readOnly = true)
    public RRespostaConsultaRegistros<TRegistro> consultar(RConsultaRegistros pConsulta) {
        RConsultaRegistros consultaNormalizada = normalizarConsulta(pConsulta);
        Specification<TEntidade> specification = criarSpecificationBase();

        for (RFiltroConsulta filtro : consultaNormalizada.filtros()) {
            specification = specification.and(new CGenericSpecification<>(mapearFiltro(filtro)));
        }

        if (consultaNormalizada.proximaEntrada() != null) {
            specification = specification.and(new COffsetSpecification<>(campoCursor(), consultaNormalizada.proximaEntrada(), consultaNormalizada.ordenacao()));
        }

        List<TEntidade> entidades = consultarEntidades(specification, consultaNormalizada);
        boolean possuiMais = entidades.size() > consultaNormalizada.limite();

        if (possuiMais) {
            entidades.remove(entidades.size() - 1);
        }

        Long proximaEntrada = entidades.isEmpty() || !possuiMais
                ? null
                : extrairProximaEntrada(entidades.get(entidades.size() - 1));

        return new RRespostaConsultaRegistros<>(
                consultaNormalizada.filtros(),
                consultaNormalizada.ordenacao(),
                consultaNormalizada.limite(),
                proximaEntrada,
                possuiMais,
                entidades.stream().map(this::paraRegistro).toList()
        );
    }

    /**
     * @description Busca um registro pelo identificador e o converte para o DTO público.
     * @param pIdRegistro - Identificador do registro solicitado.
     * @returns Registro encontrado.
     */
    @Override
    @Transactional(readOnly = true)
    public TRegistro buscarPorId(Long pIdRegistro) {
        TEntidade entidade = entityManager.find(classeEntidade, pIdRegistro);

        if (entidade == null) {
            throw new CExceptionsSystem("Registro não encontrado", HttpStatus.NOT_FOUND);
        }

        return paraRegistro(entidade);
    }

    /**
     * @description Define os campos que podem ser filtrados pelo contrato público do recurso.
     * @returns Campos aceitos nos filtros enviados pelo cliente.
     */
    protected abstract Set<String> camposFiltroPermitidos();

    /**
     * @description Define o atributo numérico e estável utilizado pelo cursor do recurso.
     * @returns Nome do atributo JPA usado no cursor.
     */
    protected abstract String campoCursor();

    /**
     * @description Converte a entidade persistida no DTO exposto pela API.
     * @param pEntidade - Entidade obtida na consulta.
     * @returns DTO público do registro.
     */
    protected abstract TRegistro paraRegistro(TEntidade pEntidade);

    /**
     * @description Extrai o identificador numérico usado como próxima entrada do cursor.
     * @param pEntidade - Última entidade devolvida na página.
     * @returns Identificador para a página seguinte.
     */
    protected abstract Long extrairProximaEntrada(TEntidade pEntidade);

    /**
     * @description Permite que o service aplique restrições invariáveis de domínio à consulta.
     * @returns Especificação inicial da consulta.
     */
    protected Specification<TEntidade> criarSpecificationBase() {
        return (pRoot, pQuery, pCriteriaBuilder) -> pCriteriaBuilder.conjunction();
    }

    /**
     * @description Converte um campo público de filtro em seu atributo correspondente na entidade.
     * @param pCampo - Campo público recebido no contrato.
     * @returns Caminho JPA autorizado para o filtro.
     */
    protected String mapearCampoFiltro(String pCampo) {
        return pCampo;
    }

    private RConsultaRegistros normalizarConsulta(RConsultaRegistros pConsulta) {
        List<RFiltroConsulta> filtros = pConsulta == null || pConsulta.filtros() == null ? List.of() : pConsulta.filtros();
        int limite = pConsulta == null || pConsulta.limite() == null ? LIMITE_PADRAO : pConsulta.limite();
        String ordenacao = pConsulta == null || pConsulta.ordenacao() == null ? "asc" : pConsulta.ordenacao().toLowerCase(Locale.ROOT);

        if (limite < 1 || limite > LIMITE_MAXIMO) {
            throw new CExceptionsSystem("O limite deve estar entre 1 e " + LIMITE_MAXIMO, HttpStatus.BAD_REQUEST);
        }

        if (!"asc".equals(ordenacao) && !"desc".equals(ordenacao)) {
            throw new CExceptionsSystem("A ordenação deve ser asc ou desc", HttpStatus.BAD_REQUEST);
        }

        return new RConsultaRegistros(
                filtros,
                ordenacao,
                limite,
                pConsulta == null ? null : pConsulta.proximaEntrada(),
                pConsulta == null || pConsulta.possuiMais() == null || pConsulta.possuiMais()
        );
    }

    private RFiltroConsulta mapearFiltro(RFiltroConsulta pFiltro) {
        if (pFiltro == null || pFiltro.campo() == null || !camposFiltroPermitidos().contains(pFiltro.campo())) {
            throw new CExceptionsSystem("O campo de filtro informado não é permitido", HttpStatus.BAD_REQUEST);
        }

        try {
            EOperadorFiltro.valueOf(pFiltro.condicao());
        } catch (IllegalArgumentException | NullPointerException pException) {
            throw new CExceptionsSystem("O operador de filtro informado não é permitido", HttpStatus.BAD_REQUEST);
        }

        return new RFiltroConsulta(
                mapearCampoFiltro(pFiltro.campo()),
                pFiltro.condicao(),
                pFiltro.valor(),
                pFiltro.dataInicio(),
                pFiltro.dataFinal(),
                pFiltro.valoresSelecionados()
        );
    }

    private List<TEntidade> consultarEntidades(Specification<TEntidade> pSpecification, RConsultaRegistros pConsulta) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<TEntidade> criteriaQuery = criteriaBuilder.createQuery(classeEntidade);
        Root<TEntidade> root = criteriaQuery.from(classeEntidade);
        Predicate predicate = pSpecification.toPredicate(root, criteriaQuery, criteriaBuilder);

        criteriaQuery.where(predicate);
        criteriaQuery.orderBy("desc".equals(pConsulta.ordenacao())
                ? criteriaBuilder.desc(root.get(campoCursor()))
                : criteriaBuilder.asc(root.get(campoCursor())));

        TypedQuery<TEntidade> query = entityManager.createQuery(criteriaQuery);
        return new ArrayList<>(query.setMaxResults(pConsulta.limite() + 1).getResultList());
    }
}
