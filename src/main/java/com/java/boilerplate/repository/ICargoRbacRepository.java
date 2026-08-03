package com.java.boilerplate.repository;

import com.java.boilerplate.model.CCargoRbac;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface ICargoRbacRepository extends IBaseRepository<CCargoRbac> {
    Optional<CCargoRbac> findByPapel(String pPapel);

    boolean existsByPapel(String pPapel);
}
