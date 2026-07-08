package com.java.boilerplate.repository;

import com.java.boilerplate.dto.common.RRespostaPaginacao;
import com.java.boilerplate.dto.filtros.RFiltroConsulta;
import com.java.boilerplate.dto.filtros.RParametrosPaginacao;
import com.java.boilerplate.repository.specifications.CGenericSpecification;
import com.java.boilerplate.repository.specifications.COffsetSpecification;
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
    default RRespostaPaginacao<T> consultarPaginado(RParametrosPaginacao pParametros, String pCampoCursor) {
        return consultarPaginado(pParametros, pCampoCursor, null);
    }

    default RRespostaPaginacao<T> consultarPaginado(
            RParametrosPaginacao pParametros,
            String pCampoCursor,
            Specification<T> pSpecificationBase
    ) {
        Specification<T> specification = pSpecificationBase == null
                ? (pRoot, pQuery, pCriteriaBuilder) -> pCriteriaBuilder.conjunction()
                : pSpecificationBase;
        RParametrosPaginacao parametros = pParametros == null
                ? new RParametrosPaginacao(null, null, null, null)
                : pParametros;

        if (parametros.filtros() != null) {
            for (RFiltroConsulta filtro : parametros.filtros()) {
                specification = specification.and(new CGenericSpecification<>(filtro));
            }
        }

        if (parametros.proximaEntrada() != null) {
            specification = specification.and(new COffsetSpecification<>(pCampoCursor, parametros.proximaEntrada(), parametros.ordem()));
        }

        int limite = parametros.limite() != null && parametros.limite() > 0 ? parametros.limite() : 20;
        Sort.Direction direction = "desc".equalsIgnoreCase(parametros.ordem()) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Page<T> pagina = findAll(specification, PageRequest.of(0, limite, Sort.by(direction, pCampoCursor)));
        List<T> items = pagina.getContent();
        Object proximaEntrada = pagina.hasNext() && !items.isEmpty()
                ? obterValorCursor(items.get(items.size() - 1), pCampoCursor)
                : null;

        return new RRespostaPaginacao<>(limite, proximaEntrada, items, pagina.hasNext());
    }

    private Object obterValorCursor(T pItem, String pCampoCursor) {
        try {
            String metodo = "get" + Character.toUpperCase(pCampoCursor.charAt(0)) + pCampoCursor.substring(1);
            return pItem.getClass().getMethod(metodo).invoke(pItem);
        } catch (ReflectiveOperationException pException) {
            return null;
        }
    }
}
