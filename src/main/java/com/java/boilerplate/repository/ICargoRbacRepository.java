package com.java.boilerplate.repository;

import com.java.boilerplate.model.CCargoRbac;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

import java.util.Optional;

@Repository
public interface ICargoRbacRepository extends IBaseRepository<CCargoRbac> {
    Optional<CCargoRbac> findByPapel(String pPapel);
    @EntityGraph(attributePaths = "permissoes")
    @Query("select c from CCargoRbac c where c.idCargo = :pId")
    Optional<CCargoRbac> findByIdWithPermissoes(@org.springframework.data.repository.query.Param("pId") Long pId);
    boolean existsByPapel(String pPapel);

    @EntityGraph(attributePaths = "permissoes")
    @Query("select distinct c from CCargoRbac c")
    List<CCargoRbac> findAllWithPermissoes();
}
