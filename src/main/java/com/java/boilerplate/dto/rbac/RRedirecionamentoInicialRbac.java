package com.java.boilerplate.dto.rbac;

import com.java.boilerplate.dto.filtros.RFiltroConsulta;

import java.util.List;

public record RRedirecionamentoInicialRbac(
        String path,
        String name,
        List<RFiltroConsulta> filtros
) {
}
