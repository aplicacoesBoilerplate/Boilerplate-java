package com.java.boilerplate.repository;

import com.java.boilerplate.model.CCargoRbac;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ICargoRbacRepository extends IBaseRepository<CCargoRbac> {
    Optional<CCargoRbac> findByPapel(String pPapel);
    boolean existsByPapel(String pPapel);
}
