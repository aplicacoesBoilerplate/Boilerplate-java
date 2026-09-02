package com.java.boilerplate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.java.boilerplate.cache.IRedisCache;
import com.java.boilerplate.enums.EComportamentoPadraoPermissao;
import com.java.boilerplate.model.CCargoRbac;
import com.java.boilerplate.model.CPermissaoCargoRbac;
import com.java.boilerplate.model.CUsuario;
import com.java.boilerplate.repository.ICargoRbacRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CRbacCacheTests {
    @Mock
    private EntityManager entityManager;
    @Mock
    private ICargoRbacRepository cargoRepository;
    @Mock
    private CAuditoriaRegistroService auditoriaRegistroService;
    @Mock
    private IRedisCache redisCache;

    private CRbacService service;
    private CUsuario usuario;

    @BeforeEach
    void configurar() {
        service = new CRbacService(entityManager, cargoRepository, auditoriaRegistroService, redisCache);
        CCargoRbac cargo = new CCargoRbac();
        cargo.setIdCargo(7L);
        cargo.setAtivo(true);
        usuario = new CUsuario();
        usuario.setCargo(cargo);
    }

    @Test
    void cacheHitDeveEvitarConsultaDePermissoesDoCargo() throws Exception {
        String permissoes = "{\"ativo\":true,\"comportamentoPadrao\":\"bloquear\",\"permissoes\":[{\"acao\":\"GET /usuarios/*\",\"liberado\":true}]}";
        when(redisCache.obter("v1:rbac:cargo:7")).thenReturn(Optional.of(permissoes));

        assertThat(service.usuarioPodeAcessarEndpoint(usuario, "GET", "/usuarios/10")).isTrue();

        verify(cargoRepository, never()).findByIdWithPermissoes(7L);
    }

    @Test
    void cacheMissDeveConsultarCargoEArmazenarPermissoes() {
        CCargoRbac cargo = new CCargoRbac();
        cargo.setIdCargo(7L);
        cargo.setAtivo(true);
        cargo.setComportamentoPadrao(EComportamentoPadraoPermissao.bloquear);
        CPermissaoCargoRbac permissao = new CPermissaoCargoRbac();
        permissao.setRecurso("api");
        permissao.setAcao("GET /usuarios/*");
        permissao.setLiberado(true);
        cargo.setPermissoes(List.of(permissao));
        when(redisCache.obter("v1:rbac:cargo:7")).thenReturn(Optional.empty());
        when(cargoRepository.findByIdWithPermissoes(7L)).thenReturn(Optional.of(cargo));

        assertThat(service.usuarioPodeAcessarEndpoint(usuario, "GET", "/usuarios/10")).isTrue();

        verify(redisCache).salvarPermanente(org.mockito.ArgumentMatchers.eq("v1:rbac:cargo:7"), org.mockito.ArgumentMatchers.anyString());
    }
}
