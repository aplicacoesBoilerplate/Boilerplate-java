package com.java.boilerplate.service;

import com.java.boilerplate.annotation.EndpointRbac;
import com.java.boilerplate.dto.rbac.REndpointManifestoRbac;
import com.java.boilerplate.dto.rbac.RManifestoRbac;
import com.java.boilerplate.dto.rbac.RRecursoManifestoRbac;
import com.java.boilerplate.enums.EAcaoRbac;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Pattern;

@Service
public class CRbacManifestoService {
    private static final int VERSAO_CONTRATO = 1;
    private static final Pattern VARIAVEL_PATH = Pattern.compile("\\{[^}]+}");
    private static final Comparator<REndpointManifestoRbac> ORDEM_ENDPOINT = Comparator
            .comparing(REndpointManifestoRbac::metodo)
            .thenComparing(REndpointManifestoRbac::path);
    private static final Set<RequestMethod> METODOS_SUPORTADOS = EnumSet.of(
            RequestMethod.GET,
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.PATCH,
            RequestMethod.DELETE
    );

    private final ObjectProvider<RequestMappingHandlerMapping> handlerMappingProvider;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private volatile RManifestoRbac manifesto;

    public CRbacManifestoService(
            @Qualifier("requestMappingHandlerMapping")
            ObjectProvider<RequestMappingHandlerMapping> pHandlerMappingProvider
    ) {
        this.handlerMappingProvider = pHandlerMappingProvider;
    }

    public RManifestoRbac buscarManifesto() {
        RManifestoRbac atual = manifesto;
        if (atual != null) {
            return atual;
        }

        synchronized (this) {
            if (manifesto == null) {
                manifesto = construirManifesto(handlerMappingProvider.getObject().getHandlerMethods());
            }
            return manifesto;
        }
    }

    private RManifestoRbac construirManifesto(Map<RequestMappingInfo, HandlerMethod> pMappings) {
        Map<String, Map<EAcaoRbac, Set<REndpointManifestoRbac>>> recursos = new TreeMap<>();

        pMappings.forEach((pMapping, pHandler) -> {
            EndpointRbac endpointRbac = AnnotatedElementUtils.findMergedAnnotation(
                    pHandler.getMethod(), EndpointRbac.class);
            if (endpointRbac == null) {
                return;
            }

            String recurso = endpointRbac.recurso().trim();
            if (recurso.isEmpty()) {
                throw new IllegalStateException("Endpoint RBAC sem recurso explícito: " + pHandler);
            }

            Set<RequestMethod> metodos = pMapping.getMethodsCondition().getMethods();
            if (metodos.isEmpty()) {
                throw new IllegalStateException("Endpoint RBAC sem método HTTP explícito: " + pHandler);
            }
            if (pMapping.getPatternValues().isEmpty()) {
                throw new IllegalStateException("Endpoint RBAC sem path explícito: " + pHandler);
            }

            Set<REndpointManifestoRbac> endpoints = recursos
                    .computeIfAbsent(recurso, pChave -> new EnumMap<>(EAcaoRbac.class))
                    .computeIfAbsent(endpointRbac.acao(), pChave -> new TreeSet<>(ORDEM_ENDPOINT));
            for (RequestMethod metodo : metodos) {
                if (!METODOS_SUPORTADOS.contains(metodo)) {
                    throw new IllegalStateException(
                            "Endpoint RBAC usa método HTTP não suportado " + metodo + ": " + pHandler);
                }
                for (String path : pMapping.getPatternValues()) {
                    endpoints.add(new REndpointManifestoRbac(metodo.name(), normalizarPath(path)));
                }
            }
        });

        LinkedHashMap<String, RRecursoManifestoRbac> resposta = new LinkedHashMap<>();
        recursos.forEach((pRecurso, pAcoes) -> {
            LinkedHashMap<String, List<REndpointManifestoRbac>> acoes = new LinkedHashMap<>();
            for (EAcaoRbac acao : EAcaoRbac.values()) {
                Set<REndpointManifestoRbac> endpoints = pAcoes.get(acao);
                if (endpoints != null && !endpoints.isEmpty()) {
                    acoes.put(acao.valor(), endpoints.stream()
                            .filter(pEndpoint -> !estaCobertoPorWildcard(pEndpoint, endpoints))
                            .toList());
                }
            }
            resposta.put(pRecurso, new RRecursoManifestoRbac(acoes));
        });
        return new RManifestoRbac(VERSAO_CONTRATO, resposta);
    }

    private String normalizarPath(String pPath) {
        return VARIAVEL_PATH.matcher(pPath).replaceAll("**");
    }

    private boolean estaCobertoPorWildcard(
            REndpointManifestoRbac pEndpoint,
            Set<REndpointManifestoRbac> pEndpoints
    ) {
        if (pEndpoint.path().contains("*")) {
            return false;
        }

        return pEndpoints.stream().anyMatch(pCandidato ->
                pCandidato.metodo().equals(pEndpoint.metodo())
                        && pCandidato.path().endsWith("/**")
                        && antPathMatcher.match(pCandidato.path(), pEndpoint.path()));
    }
}
