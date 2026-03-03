package com.java.boilerplate.repository.specifications;

import com.java.boilerplate.enums.FilterOperator;
import com.java.boilerplate.model.pagination.RequestFilters;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class GenericSpecification<T> implements Specification<T> {

    private final RequestFilters filter;

    public GenericSpecification(RequestFilters filter) {
        this.filter = filter;
    }

    @Override
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
        Path<?> path;
        String field = filter.getField();

        if (field.contains(".")) {
            String[] parts = field.split("\\.");
            Join<Object, Object> join = root.join(parts[0]);
            path = join.get(parts[1]);
        } else {
            path = root.get(field);
        }

        FilterOperator operator = FilterOperator.valueOf(filter.getCondition());

        return switch (operator) {
            // Operadores de Texto
            case contains -> cb.like(cb.lower(path.as(String.class)), "%" + filter.getValue().toLowerCase() + "%");
            case startsIn -> cb.like(cb.lower(path.as(String.class)), filter.getValue().toLowerCase() + "%");
            case endsIn -> cb.like(cb.lower(path.as(String.class)), "%" + filter.getValue().toLowerCase());

            // Operadores Lógicos/Numéricos
            case equals -> cb.equal(path, filter.getValue());
            case notEquals -> cb.notEqual(path, filter.getValue());
            case greaterThan -> cb.greaterThan(path.as(Comparable.class), (Comparable) filter.getValue());
            case greaterEqual -> cb.greaterThanOrEqualTo(path.as(Comparable.class), (Comparable) filter.getValue());
            case lessThan -> cb.lessThan(path.as(Comparable.class), (Comparable) filter.getValue());
            case lessEqual -> cb.lessThanOrEqualTo(path.as(Comparable.class), (Comparable) filter.getValue());

            // Operadores de Intervalo (Datas/Números)
            case between -> cb.between(path.as(LocalDateTime.class), filter.getStartDate(), filter.getEndDate());

            // Operadores de Coleção (Select/Except)
            case select -> path.in(filter.getSelectValues());
            case except -> cb.not(path.in(filter.getSelectValues()));

            default -> cb.conjunction();
        };
    }
}
