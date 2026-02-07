package com.java.boilerplate.dto.users;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.java.boilerplate.enums.GenderUser;
import com.java.boilerplate.enums.UserRoles;
import com.java.boilerplate.model.Users;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.locationtech.jts.geom.Point;

public record DTOUser(
        @JsonView(UserViews.Internal.class)
        Long idUser,

        @NotBlank(message = "The full name field is required")
        @JsonView(UserViews.Public.class)
        String fullName,

        @NotBlank(message = "The username field is required")
        @JsonView(UserViews.Public.class)
        String username,

        @JsonView(UserViews.Public.class)
        String bio,

        @NotNull(message = "The gender field is required")
        @JsonView(UserViews.Internal.class)
        GenderUser userGender,

        @JsonView(UserViews.Public.class)
        String avatarUrl,

        @JsonView(UserViews.Internal.class)
        Boolean showWppNumber,

        @JsonView(UserViews.Public.class)
        String phoneNumber,

        @NotBlank(message = "The email field is required")
        @Email(message = "The email field is invalid")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String email,

        @NotBlank(message = "The password field is required")
        @Size(min = 8, max = 20, message = "The password field must be between 8 and 20 characters long")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String password,

        @NotNull(message = "The user's role was not specified")
        @JsonView(UserViews.Internal.class)
        UserRoles role,

        @NotNull(message = "The field location is required")
        @JsonView(UserViews.Public.class)
        DTOLocation location
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

        if (this.location() != null) {
            org.locationtech.jts.geom.GeometryFactory factory =
                    new org.locationtech.jts.geom.GeometryFactory(new org.locationtech.jts.geom.PrecisionModel(), 4326);

            org.locationtech.jts.geom.Point point = factory.createPoint(
                    new org.locationtech.jts.geom.Coordinate(this.location().longitude(), this.location().latitude())
            );
            user.setLocation(point);
        }

        return user;
    }

    public static DTOUser fromEntity(Users user) {
        Point p = user.getLocation();
        DTOLocation location = new DTOLocation(p.getY(), p.getX());

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
                location
        );
    }
}
