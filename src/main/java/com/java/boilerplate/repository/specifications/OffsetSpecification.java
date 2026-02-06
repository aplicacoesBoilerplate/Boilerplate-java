package com.java.boilerplate.repository.specifications;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

public class OffsetSpecification<T> implements Specification<T> {

    private final String field;
    private final Object offset;

    public OffsetSpecification(String field, Object offset) {
        this.field = field;
        this.offset = offset;
    }

    @Override
    public Predicate toPredicate(
            Root<T> root,
            CriteriaQuery<?> query,
            CriteriaBuilder cb
    ) {
        if (offset == null) {
            return cb.conjunction();
        }

        return cb.greaterThan(
                root.get(field).as(Comparable.class),
                (Comparable) offset
        );
    }
}