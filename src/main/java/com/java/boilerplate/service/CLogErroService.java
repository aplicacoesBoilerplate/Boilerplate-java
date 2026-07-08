package com.java.boilerplate.service;

import com.java.boilerplate.dto.common.RRespostaPaginacao;
import com.java.boilerplate.dto.errors.RLogErro;
import com.java.boilerplate.dto.filtros.RParametrosPaginacao;
import com.java.boilerplate.model.CLogErro;
import com.java.boilerplate.repository.ILogErroRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CLogErroService {
    private final ILogErroRepository logErroRepository;

    public CLogErroService(ILogErroRepository pLogErroRepository) {
        this.logErroRepository = pLogErroRepository;
    }

    @Transactional(readOnly = true)
    public RRespostaPaginacao<RLogErro> consultar(RParametrosPaginacao pParametros) {
        RRespostaPaginacao<CLogErro> pagina = logErroRepository.consultarPaginado(pParametros, "idError");
        return new RRespostaPaginacao<>(
                pagina.limite(),
                pagina.proximaEntrada(),
                pagina.items().stream().map(RLogErro::fromEntity).toList(),
                pagina.temMaisRegistros()
        );
    }
}
