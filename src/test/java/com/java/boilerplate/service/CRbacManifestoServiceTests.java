package com.java.boilerplate.service;

import com.java.boilerplate.annotation.EndpointRbac;
import com.java.boilerplate.dto.rbac.REndpointManifestoRbac;
import com.java.boilerplate.dto.rbac.RManifestoRbac;
import com.java.boilerplate.enums.EAcaoRbac;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CRbacManifestoServiceTests {
    private RequestMappingHandlerMapping handlerMapping;
    private CRbacManifestoService service;

    @BeforeEach
    void configurar() {
        handlerMapping = mock(RequestMappingHandlerMapping.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<RequestMappingHandlerMapping> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(handlerMapping);
        service = new CRbacManifestoService(provider);
    }

    @Test
    void deveAgruparNormalizarDeduplicarOrdenarEManterManifestoEmCache() throws Exception {
        Map<RequestMappingInfo, HandlerMethod> mappings = new LinkedHashMap<>();
        mappings.put(
                RequestMappingInfo.paths("/usuarios/{pIdUsuario}", "/usuarios/{codigo}")
                        .methods(org.springframework.web.bind.annotation.RequestMethod.GET).build(),
                handler("consultar"));
        mappings.put(
                RequestMappingInfo.paths("/usuarios/consulta")
                        .methods(org.springframework.web.bind.annotation.RequestMethod.POST).build(),
                handler("consultar"));
        mappings.put(
                RequestMappingInfo.paths("/usuarios")
                        .methods(org.springframework.web.bind.annotation.RequestMethod.GET).build(),
                handler("consultar"));
        mappings.put(
                RequestMappingInfo.paths("/usuarios")
                        .methods(org.springframework.web.bind.annotation.RequestMethod.POST).build(),
                handler("gravar"));
        mappings.put(
                RequestMappingInfo.paths("/interno")
                        .methods(org.springframework.web.bind.annotation.RequestMethod.DELETE).build(),
                handler("naoAnotado"));
        when(handlerMapping.getHandlerMethods()).thenReturn(mappings);

        RManifestoRbac manifesto = service.buscarManifesto();

        assertThat(manifesto.versaoContrato()).isEqualTo(1);
        assertThat(manifesto.recursos()).containsOnlyKeys("Usuarios");
        assertThat(manifesto.recursos().get("Usuarios").acoes()).containsOnlyKeys("consultar", "gravar");
        assertThat(manifesto.recursos().get("Usuarios").acoes().get("consultar"))
                .containsExactly(
                        new REndpointManifestoRbac(HttpMethod.GET.name(), "/usuarios/**"),
                        new REndpointManifestoRbac(HttpMethod.POST.name(), "/usuarios/consulta"));
        assertThat(manifesto.recursos().get("Usuarios").acoes().get("gravar"))
                .containsExactly(new REndpointManifestoRbac(HttpMethod.POST.name(), "/usuarios"));

        assertThat(service.buscarManifesto()).isSameAs(manifesto);
        verify(handlerMapping).getHandlerMethods();
    }

    @Test
    void deveRejeitarEndpointRbacSemMetodoHttpExplicito() throws Exception {
        when(handlerMapping.getHandlerMethods()).thenReturn(Map.of(
                RequestMappingInfo.paths("/usuarios").build(),
                handler("consultar")));

        assertThatThrownBy(service::buscarManifesto)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("método HTTP explícito");
    }

    @Test
    void deveRejeitarMetodoHttpForaDoContratoDoFrontend() throws Exception {
        when(handlerMapping.getHandlerMethods()).thenReturn(Map.of(
                RequestMappingInfo.paths("/usuarios")
                        .methods(org.springframework.web.bind.annotation.RequestMethod.HEAD).build(),
                handler("consultar")));

        assertThatThrownBy(service::buscarManifesto)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("método HTTP não suportado")
                .hasMessageContaining("HEAD");
    }

    private HandlerMethod handler(String pNomeMetodo) throws NoSuchMethodException {
        Method method = CControllerTeste.class.getDeclaredMethod(pNomeMetodo);
        return new HandlerMethod(new CControllerTeste(), method);
    }

    private static class CControllerTeste {
        @EndpointRbac(recurso = "Usuarios", acao = EAcaoRbac.CONSULTAR)
        void consultar() {}

        @EndpointRbac(recurso = "Usuarios", acao = EAcaoRbac.GRAVAR)
        void gravar() {}

        void naoAnotado() {}
    }
}
