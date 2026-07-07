package com.java.boilerplate.repository;

import com.java.boilerplate.model.CPermissaoCargoRbac;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IPermissaoCargoRbacRepository extends IBaseRepository<CPermissaoCargoRbac> {
    List<CPermissaoCargoRbac> findByCargo_IdCargoAndRecurso(Long pIdCargo, String pRecurso);
}
