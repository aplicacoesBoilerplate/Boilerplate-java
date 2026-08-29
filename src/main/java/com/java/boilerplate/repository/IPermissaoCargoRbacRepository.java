package com.java.boilerplate.repository;

import com.java.boilerplate.model.CPermissaoCargoRbac;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface IPermissaoCargoRbacRepository extends IBaseRepository<CPermissaoCargoRbac> {
    List<CPermissaoCargoRbac> findByCargo_IdCargoAndRecurso(Long pIdCargo, String pRecurso);
}
