package com.java.boilerplate.repository.specifications;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
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

        Comparable valor = converterOffset();
        if ("desc".equalsIgnoreCase(ordem)) {
            return pCriteriaBuilder.lessThan(pRoot.get(campo).as(Comparable.class), valor);
        }

        return pCriteriaBuilder.greaterThan(pRoot.get(campo).as(Comparable.class), valor);
    }

    private Comparable converterOffset() {
        if (offset instanceof Number number) {
            return number.longValue();
        }

        return Long.valueOf(String.valueOf(offset));
    }
}
