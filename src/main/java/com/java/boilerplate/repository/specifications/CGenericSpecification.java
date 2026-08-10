package com.java.boilerplate.repository.specifications;

import com.java.boilerplate.dto.filtros.RFiltroConsulta;
import com.java.boilerplate.enums.EOperadorFiltro;
import com.java.boilerplate.exception.CExceptionsSystem;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * @description Constrói predicates JPA a partir dos filtros validados pelo service consumidor.
 * @template TEntidade - Tipo da entidade filtrada.
 */
public class CGenericSpecification<TEntidade> implements Specification<TEntidade> {
    private final RFiltroConsulta filtro;

    public CGenericSpecification(RFiltroConsulta pFiltro) {
        this.filtro = pFiltro;
    }

    @Override
    public Predicate toPredicate(Root<TEntidade> pRoot, CriteriaQuery<?> pQuery, CriteriaBuilder pCriteriaBuilder) {
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
            case maiorQue, maiorIgual, menorQue, menorIgual -> criarComparacao(pCriteriaBuilder, path, operador, filtro.valor());
            case entre -> criarIntervalo(pCriteriaBuilder, path, filtro.dataInicio(), filtro.dataFinal());
            case selecao -> path.in(valoresSelecionadosConvertidos(path));
            case excecao -> pCriteriaBuilder.not(path.in(valoresSelecionadosConvertidos(path)));
        };
    }

    private Path<?> resolverPath(Root<TEntidade> pRoot, String pCampo) {
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
            return converterDataHora(pValor);
        }

        if (Enum.class.isAssignableFrom(type)) {
            return Arrays.stream(type.getEnumConstants())
                    .filter(pEnum -> ((Enum<?>) pEnum).name().equals(valor))
                    .findFirst()
                    .orElseThrow(() -> new CExceptionsSystem("O valor informado não pertence ao enum do campo filtrado", HttpStatus.BAD_REQUEST));
        }

        return pValor;
    }

    private Predicate criarComparacao(
            CriteriaBuilder pCriteriaBuilder,
            Path<?> pPath,
            EOperadorFiltro pOperador,
            Object pValor
    ) {
        Class<?> type = pPath.getJavaType();

        if (Long.class.equals(type) || long.class.equals(type)) {
            Long valorConvertido = ((Number) converterValor(pPath, pValor)).longValue();
            return compararLong(pCriteriaBuilder, pPath.as(Long.class), pOperador, valorConvertido);
        }

        if (Integer.class.equals(type) || int.class.equals(type)) {
            Integer valorConvertido = ((Number) converterValor(pPath, pValor)).intValue();
            return compararInteger(pCriteriaBuilder, pPath.as(Integer.class), pOperador, valorConvertido);
        }

        if (LocalDateTime.class.equals(type)) {
            return compararDataHora(pCriteriaBuilder, pPath.as(LocalDateTime.class), pOperador, converterDataHora(pValor));
        }

        if (String.class.equals(type)) {
            return compararTexto(pCriteriaBuilder, pPath.as(String.class), pOperador, texto(pValor));
        }

        throw new CExceptionsSystem("O operador informado não é compatível com o campo filtrado", HttpStatus.BAD_REQUEST);
    }

    private Predicate criarIntervalo(
            CriteriaBuilder pCriteriaBuilder,
            Path<?> pPath,
            LocalDateTime pDataInicio,
            LocalDateTime pDataFinal
    ) {
        if (!LocalDateTime.class.equals(pPath.getJavaType()) || pDataInicio == null || pDataFinal == null) {
            throw new CExceptionsSystem("O operador entre exige um campo e datas compatíveis", HttpStatus.BAD_REQUEST);
        }

        return pCriteriaBuilder.between(pPath.as(LocalDateTime.class), pDataInicio, pDataFinal);
    }

    private Predicate compararLong(CriteriaBuilder pCriteriaBuilder, Expression<Long> pCampo, EOperadorFiltro pOperador, Long pValor) {
        return switch (pOperador) {
            case maiorQue -> pCriteriaBuilder.greaterThan(pCampo, pValor);
            case maiorIgual -> pCriteriaBuilder.greaterThanOrEqualTo(pCampo, pValor);
            case menorQue -> pCriteriaBuilder.lessThan(pCampo, pValor);
            case menorIgual -> pCriteriaBuilder.lessThanOrEqualTo(pCampo, pValor);
            default -> throw new IllegalArgumentException("Operador relacional inválido");
        };
    }

    private Predicate compararInteger(CriteriaBuilder pCriteriaBuilder, Expression<Integer> pCampo, EOperadorFiltro pOperador, Integer pValor) {
        return switch (pOperador) {
            case maiorQue -> pCriteriaBuilder.greaterThan(pCampo, pValor);
            case maiorIgual -> pCriteriaBuilder.greaterThanOrEqualTo(pCampo, pValor);
            case menorQue -> pCriteriaBuilder.lessThan(pCampo, pValor);
            case menorIgual -> pCriteriaBuilder.lessThanOrEqualTo(pCampo, pValor);
            default -> throw new IllegalArgumentException("Operador relacional inválido");
        };
    }

    private Predicate compararDataHora(CriteriaBuilder pCriteriaBuilder, Expression<LocalDateTime> pCampo, EOperadorFiltro pOperador, LocalDateTime pValor) {
        return switch (pOperador) {
            case maiorQue -> pCriteriaBuilder.greaterThan(pCampo, pValor);
            case maiorIgual -> pCriteriaBuilder.greaterThanOrEqualTo(pCampo, pValor);
            case menorQue -> pCriteriaBuilder.lessThan(pCampo, pValor);
            case menorIgual -> pCriteriaBuilder.lessThanOrEqualTo(pCampo, pValor);
            default -> throw new IllegalArgumentException("Operador relacional inválido");
        };
    }

    private Predicate compararTexto(CriteriaBuilder pCriteriaBuilder, Expression<String> pCampo, EOperadorFiltro pOperador, String pValor) {
        return switch (pOperador) {
            case maiorQue -> pCriteriaBuilder.greaterThan(pCampo, pValor);
            case maiorIgual -> pCriteriaBuilder.greaterThanOrEqualTo(pCampo, pValor);
            case menorQue -> pCriteriaBuilder.lessThan(pCampo, pValor);
            case menorIgual -> pCriteriaBuilder.lessThanOrEqualTo(pCampo, pValor);
            default -> throw new IllegalArgumentException("Operador relacional inválido");
        };
    }

    private LocalDateTime converterDataHora(Object pValor) {
        if (pValor instanceof LocalDateTime dataHora) {
            return dataHora;
        }

        try {
            return LocalDateTime.parse(String.valueOf(pValor));
        } catch (DateTimeParseException pException) {
            throw new CExceptionsSystem("A data informada no filtro é inválida", HttpStatus.BAD_REQUEST);
        }
    }
}
