package com.java.boilerplate.repository.specifications;

import com.java.boilerplate.dto.filtros.RFiltroConsulta;
import com.java.boilerplate.enums.EOperadorFiltro;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

public class CGenericSpecification<T> implements Specification<T> {
    private final RFiltroConsulta filtro;

    public CGenericSpecification(RFiltroConsulta pFiltro) {
        this.filtro = pFiltro;
    }

    @Override
    public Predicate toPredicate(Root<T> pRoot, CriteriaQuery<?> pQuery, CriteriaBuilder pCriteriaBuilder) {
        if (filtro == null || filtro.campo() == null || filtro.campo().isBlank() || filtro.condicao() == null) {
            return pCriteriaBuilder.conjunction();
        }

        Path<?> path = resolverPath(pRoot, filtro.campo());
        EOperadorFiltro operador = EOperadorFiltro.valueOf(filtro.condicao());

        return switch (operador) {
            case contem -> pCriteriaBuilder.like(pCriteriaBuilder.lower(path.as(String.class)), "%" + texto(filtro.valor()) + "%");
            case naoContem -> pCriteriaBuilder.notLike(pCriteriaBuilder.lower(path.as(String.class)), "%" + texto(filtro.valor()) + "%");
            case comecaCom -> pCriteriaBuilder.like(pCriteriaBuilder.lower(path.as(String.class)), texto(filtro.valor()) + "%");
            case terminaCom -> pCriteriaBuilder.like(pCriteriaBuilder.lower(path.as(String.class)), "%" + texto(filtro.valor()));
            case igual -> pCriteriaBuilder.equal(path, converterValor(path, filtro.valor()));
            case diferente -> pCriteriaBuilder.notEqual(path, converterValor(path, filtro.valor()));
            case verdadeiro -> pCriteriaBuilder.isTrue(path.as(Boolean.class));
            case falso -> pCriteriaBuilder.isFalse(path.as(Boolean.class));
            case maiorQue -> pCriteriaBuilder.greaterThan(path.as(Comparable.class), (Comparable) converterValor(path, filtro.valor()));
            case maiorIgual -> pCriteriaBuilder.greaterThanOrEqualTo(path.as(Comparable.class), (Comparable) converterValor(path, filtro.valor()));
            case menorQue -> pCriteriaBuilder.lessThan(path.as(Comparable.class), (Comparable) converterValor(path, filtro.valor()));
            case menorIgual -> pCriteriaBuilder.lessThanOrEqualTo(path.as(Comparable.class), (Comparable) converterValor(path, filtro.valor()));
            case entre -> criarFiltroEntre(path, pCriteriaBuilder);
            case selecao -> path.in(valoresSelecionadosConvertidos(path));
            case excecao -> pCriteriaBuilder.not(path.in(valoresSelecionadosConvertidos(path)));
        };
    }

    private Path<?> resolverPath(Root<T> pRoot, String pCampo) {
        if (!pCampo.contains(".")) {
            return pRoot.get(pCampo);
        }

        String[] partes = pCampo.split("\\.");
        Path<?> path = pRoot.get(partes[0]);
        for (int index = 1; index < partes.length; index++) {
            path = path.get(partes[index]);
        }
        return path;
    }

    private String texto(Object pValor) {
        return String.valueOf(pValor == null ? "" : pValor).toLowerCase();
    }

    private List<Object> valoresSelecionadosConvertidos(Path<?> pPath) {
        return filtro.valoresSelecionados() == null
                ? List.of()
                : filtro.valoresSelecionados().stream().map(pValor -> converterValor(pPath, pValor)).toList();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Predicate criarFiltroEntre(Path<?> pPath, CriteriaBuilder pCriteriaBuilder) {
        if (LocalDateTime.class.isAssignableFrom(pPath.getJavaType())) {
            if (filtro.dataInicio() == null || filtro.dataFinal() == null) {
                return pCriteriaBuilder.disjunction();
            }

            return pCriteriaBuilder.between(pPath.as(LocalDateTime.class), filtro.dataInicio(), filtro.dataFinal());
        }

        List<Object> valoresIntervalo = filtro.valor() instanceof List<?> valores
                ? valores.stream().map(pValor -> (Object) pValor).toList()
                : List.of();

        if (valoresIntervalo.size() < 2) {
            return pCriteriaBuilder.disjunction();
        }

        Comparable valorInicial = (Comparable) converterValor(pPath, valoresIntervalo.get(0));
        Comparable valorFinal = (Comparable) converterValor(pPath, valoresIntervalo.get(1));

        return pCriteriaBuilder.between(pPath.as(Comparable.class), valorInicial, valorFinal);
    }

    private Object converterValor(Path<?> pPath, Object pValor) {
        if (pValor == null) {
            return null;
        }

        Class<?> type = pPath.getJavaType();
        String valor = String.valueOf(pValor);

        if (Boolean.class.equals(type) || boolean.class.equals(type)) {
            return Boolean.valueOf(valor);
        }

        if (Long.class.equals(type) || long.class.equals(type)) {
            return Long.valueOf(valor);
        }

        if (Integer.class.equals(type) || int.class.equals(type)) {
            return Integer.valueOf(valor);
        }

        if (LocalDateTime.class.equals(type)) {
            return converterDataHora(valor);
        }

        if (Enum.class.isAssignableFrom(type)) {
            return Enum.valueOf((Class<Enum>) type.asSubclass(Enum.class), valor);
        }

        return pValor;
    }

    private LocalDateTime converterDataHora(String pValor) {
        try {
            return LocalDateTime.parse(pValor);
        } catch (DateTimeParseException pException) {
            return LocalDate.parse(pValor).atStartOfDay();
        }
    }
}
