package com.java.boilerplate.dto.users;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.java.boilerplate.model.UserSubscription;
import com.fasterxml.jackson.annotation.JsonView;
import com.java.boilerplate.enums.GenderUser;
import com.java.boilerplate.enums.UserRoles;
import com.java.boilerplate.model.Users;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.Coordinate;

public record DTOUser(
        @JsonView(UserViews.Internal.class)
        Long idUser,

        @NotBlank(message = "O campo nome é obrigatório")
        @JsonView(UserViews.Public.class)
        String fullName,

        @NotBlank(message = "O campo nome de usuário é obrigatório")
        @JsonView(UserViews.Public.class)
        String username,

        @JsonView(UserViews.Public.class)
        String bio,

        @JsonView(UserViews.Public.class)
        GenderUser userGender,

        @JsonView(UserViews.Public.class)
        String avatarUrl,

        @JsonView(UserViews.Internal.class)
        Boolean showWppNumber,

        @JsonView(UserViews.Public.class)
        String phoneNumber,

        @Email(message = "Formato de e-mail inválido")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String email,

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String password,

        @NotNull(message = "O nível de permissão não foi específicado")
        @JsonView(UserViews.Internal.class)
        UserRoles role,

        @NotNull(message = "A informação da localização é obrigatória")
        @JsonView(UserViews.Public.class)
        DTOLocation location,

        @JsonView(UserViews.Public.class)
        Boolean isOnline,

        @JsonView(UserViews.Public.class)
        Boolean isActive,

        @JsonView(UserViews.Public.class)
        Boolean hasActiveSubscription
) {
    public Users toEntity() {
        Users user = new Users();
        user.setIdUser(this.idUser());
        user.setFullName(this.fullName());
        user.setUserUsername(this.username());
        user.setBio(this.bio());
        user.setUserGender(this.userGender());
        user.setAvatarUrl(this.avatarUrl());
        user.setShowWppNumber(this.showWppNumber());
        user.setPhoneNumber(this.phoneNumber());
        user.setEmail(this.email());
        user.setPassword(this.password());
        user.setRole(this.role());
        user.setIsOnline(this.isOnline);
        user.setIsActive(this.isActive);

        if (this.location() != null) {
            GeometryFactory factory =
                    new GeometryFactory(new PrecisionModel(), 4326);

            Point point = factory.createPoint(
                    new Coordinate(this.location().longitude(), this.location().latitude())
            );
            user.setLocation(point);
        }

        return user;
    }

    public static DTOUser fromEntity(Users user) {
        Point p = user.getLocation();
        DTOLocation location = new DTOLocation(p.getY(), p.getX());

        boolean hasActiveSubscription = user.getRole() == UserRoles.ADMIN ||
                (user.getSubscription() != null && user.getSubscription().isValid());

        return new DTOUser(
                user.getIdUser(),
                user.getFullName(),
                user.getUserUsername(),
                user.getBio(),
                user.getUserGender(),
                user.getAvatarUrl(),
                user.getShowWppNumber(),
                user.getPhoneNumber(),
                user.getEmail(),
                null,
                user.getRole(),
                location,
                user.getIsOnline(),
                user.getIsActive(),
                hasActiveSubscription
        );
    }
}
