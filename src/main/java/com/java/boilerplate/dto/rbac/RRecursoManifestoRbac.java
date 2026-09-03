package com.java.boilerplate.dto.rbac;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record RRecursoManifestoRbac(Map<String, List<REndpointManifestoRbac>> acoes) {
    public RRecursoManifestoRbac {
        LinkedHashMap<String, List<REndpointManifestoRbac>> copia = new LinkedHashMap<>();
        acoes.forEach((pAcao, pEndpoints) -> copia.put(pAcao, List.copyOf(pEndpoints)));
        acoes = Collections.unmodifiableMap(copia);
    }
}
