package com.java.boilerplate.dto.rbac;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record RManifestoRbac(int versaoContrato, Map<String, RRecursoManifestoRbac> recursos) {
    public RManifestoRbac {
        recursos = Collections.unmodifiableMap(new LinkedHashMap<>(recursos));
    }
}
