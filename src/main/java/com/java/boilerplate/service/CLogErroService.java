package com.java.boilerplate.service;

import com.java.boilerplate.dto.errors.RLogErro;
import com.java.boilerplate.model.CLogErro;
import com.java.boilerplate.service.base.CBaseConsultaService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class CLogErroService extends CBaseConsultaService<CLogErro, RLogErro> {
    public CLogErroService(EntityManager pEntityManager) {
        super(pEntityManager, CLogErro.class);
    }

    @Override
    protected Set<String> camposFiltroPermitidos() {
        return Set.of("idErro", "mensagem", "arquivo", "classe", "metodo", "linha", "httpStatusCode", "idUsuario", "usuarioReferencia", "dataHora");
    }

    @Override
    protected String campoCursor() {
        return "idErro";
    }

    @Override
    protected RLogErro paraRegistro(CLogErro pLogErro) {
        return RLogErro.fromEntity(pLogErro);
    }

    @Override
    protected Long extrairProximaEntrada(CLogErro pLogErro) {
        return pLogErro.getIdErro();
    }

    @Override
    protected String mapearCampoFiltro(String pCampo) {
        return switch (pCampo) {
            case "idErro" -> "idErro";
            case "idUsuario" -> "usuario.idUsuario";
            default -> pCampo;
        };
    }
}
