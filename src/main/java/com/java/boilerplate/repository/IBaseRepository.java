package com.java.boilerplate.repository;

import com.java.boilerplate.dto.DTOPagination;
import com.java.boilerplate.model.pagination.RequestFilters;
import com.java.boilerplate.model.pagination.RequestPagination;
import com.java.boilerplate.repository.specifications.GenericSpecification;
import com.java.boilerplate.repository.specifications.OffsetSpecification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

@NoRepositoryBean
public interface IBaseRepository<T> extends JpaRepository<T, Long>, JpaSpecificationExecutor<T> {

    default DTOPagination<T> findPaginationItens(RequestPagination request, String offsetField) {
        Specification<T> spec = Specification.where((Specification<T>) null);

        if (request.getFilters() != null) {
            for (RequestFilters filter : request.getFilters()) {
                spec = spec.and(new GenericSpecification<>(filter));
            }
        }

        if (request.getNextEntry() > 0) {
            spec = spec.and(new OffsetSpecification<>(offsetField, request.getNextEntry()));
        }

        PageRequest pageRequest = PageRequest.of(0, request.getLimit(), Sort.by(Sort.Direction.ASC, offsetField));

        Page<T> page = findAll(spec, pageRequest);

        return new DTOPagination<>(
                request.getLimit(),
                request.getNextEntry(),
                (int) page.getTotalElements(),
                page.hasNext(),
                page.getContent()
        );
    }

    default Specification<T> createGlobalSearch(String searchTerm, List<String> fields) {
        return (root, query, cb) -> {
            if (searchTerm == null || searchTerm.isEmpty()) return cb.conjunction();

            List<Predicate> predicates = fields.stream()
                    .map(field -> cb.like(cb.lower(root.get(field).as(String.class)), "%" + searchTerm.toLowerCase() + "%"))
                    .toList();

            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}
