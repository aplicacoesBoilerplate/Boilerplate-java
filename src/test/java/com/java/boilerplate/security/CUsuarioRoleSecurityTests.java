package com.java.boilerplate.security;

import com.java.boilerplate.model.CCargoRbac;
import com.java.boilerplate.model.CUsuario;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CUsuarioRoleSecurityTests {
    @Test
    void cargoInativoNaoDeveAutenticarNemConcederRole() {
        CCargoRbac cargo = new CCargoRbac();
        cargo.setPapel("ADMIN");
        cargo.setAtivo(false);
        CUsuario usuario = new CUsuario();
        usuario.setAtivo(true);
        usuario.setCargo(cargo);

        assertThat(usuario.isEnabled()).isFalse();
        assertThat(usuario.getAuthorities()).isEmpty();
    }
}
