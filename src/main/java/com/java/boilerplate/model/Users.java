package com.java.boilerplate.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.java.boilerplate.enums.GenderUser;
import com.java.boilerplate.enums.UserRoles;
import com.java.boilerplate.modelQueryJPA.UsersQueriesJPA;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.locationtech.jts.geom.Point;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users_app")
@Data
@EqualsAndHashCode(callSuper = true)
public class Users extends UsersQueriesJPA implements UserDetails {
    @Id
    @Column(name = "id_user")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idUser;

    @Column(name = "user_full_name", nullable = false)
    private String fullName;

    @Column(name = "user_username", nullable = false, length = 20)
    private String userUsername;

    @Column(name = "user_bio")
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_gender", nullable = false)
    private GenderUser userGender;

    @Column(name = "user_avatar_url")
    private String avatarUrl;

    @Column(name = "user_show_wpp_number")
    private Boolean showWppNumber;

    @Column(name = "user_phone_number")
    private String phoneNumber;

    @Column(name = "user_email", nullable = false, length = 150)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "user_password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    private UserRoles role;

    @Column(name = "user_location", columnDefinition = "POINT SRID 4326", nullable = false)
    private Point location;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChatContacts> contacts;

    @JsonIgnore
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Gallery> galleryPhotos;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if(this.role == UserRoles.ADMIN) return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"));
        else return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public String getUsername() { return email; }
    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }
}
