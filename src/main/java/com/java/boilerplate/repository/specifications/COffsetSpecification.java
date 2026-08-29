package com.java.boilerplate.repository.specifications;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

public class COffsetSpecification<T> implements Specification<T> {
    private final String campo;
    private final Object offset;
    private final String ordem;

    public COffsetSpecification(String pCampo, Object pOffset, String pOrdem) {
        this.campo = pCampo;
        this.offset = pOffset;
        this.ordem = pOrdem;
    }

    @Override
    public Predicate toPredicate(Root<T> pRoot, CriteriaQuery<?> pQuery, CriteriaBuilder pCriteriaBuilder) {
        if (offset == null) {
            return pCriteriaBuilder.conjunction();
        }

        Expression<Long> campoCursor = pRoot.get(campo).as(Long.class);
        Long valor = converterOffset();
        if ("desc".equalsIgnoreCase(ordem)) {
            return pCriteriaBuilder.lessThan(campoCursor, valor);
        }

        return pCriteriaBuilder.greaterThan(campoCursor, valor);
    }

    private Long converterOffset() {
        if (offset instanceof Number number) {
            return number.longValue();
        }

        return Long.valueOf(String.valueOf(offset));
    }
}
