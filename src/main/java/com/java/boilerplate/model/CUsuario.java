package com.java.boilerplate.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * @description Entidade central de usuário usada para autenticação, administração e vínculos de RBAC.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class CUsuario extends CEntidadeAuditavel implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @JsonIgnore
    @Column(name = "senha", nullable = false)
    private String senha;

    @Column(name = "avatar", length = 500)
    private String avatar;

    @Column(name = "telefone", length = 30)
    private String telefone;

    @Column(name = "notificar", nullable = false)
    private Boolean notificar = false;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_cargo", nullable = false)
    private CCargoRbac cargo;

    public String getPapel() {
        return cargo == null ? null : cargo.getPapel();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (cargo == null || cargo.getPapel() == null) {
            return List.of();
        }

        return List.of(new SimpleGrantedAuthority("ROLE_" + cargo.getPapel()));
    }

    @Override
    public String getPassword() {
        return senha;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(ativo);
    }
}
