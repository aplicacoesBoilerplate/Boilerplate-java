package com.java.boilerplate.dto.users;

import com.fasterxml.jackson.annotation.JsonView;
import com.java.boilerplate.enums.GenderUser;
import com.java.boilerplate.enums.UserRoles;
import com.java.boilerplate.model.Users;
import jakarta.validation.constraints.Email;
import org.springframework.data.geo.Point;

public record DTOUser(
        @JsonView(UserViews.Public.class)
        Long idUser,

        @JsonView(UserViews.Public.class)
        String fullName,

        @JsonView(UserViews.Public.class)
        String username,

        @JsonView(UserViews.Public.class)
        String bio,

        @JsonView(UserViews.Registration.class)
        GenderUser userGender,

        @JsonView(UserViews.Public.class)
        String avatarUrl,

        @JsonView(UserViews.Public.class)
        Boolean showWppNumber,

        @JsonView(UserViews.Public.class)
        String phoneNumber,

        @JsonView(UserViews.Internal.class)
        @Email
        String email,

        @JsonView(UserViews.Registration.class)
        String password,

        @JsonView(UserViews.Internal.class)
        UserRoles role,

        @JsonView(UserViews.Public.class)
        Point location
) {
    public static Users toEntity(DTOUser data) {
        Users user = new Users();
        user.setIdUser(data.idUser());
        user.setFullName(data.fullName());
        user.setUsername(data.username());
        user.setBio(data.bio());
        user.setUserGender(data.userGender());
        user.setAvatarUrl(data.avatarUrl());
        user.setShowWppNumber(data.showWppNumber());
        user.setPhoneNumber(data.phoneNumber());
        user.setEmail(data.email());
        user.setPassword(data.password());
        user.setRole(data.role());
        user.setLocation(data.location());
        return user;
    }

    public static DTOUser fromEntity(Users user) {
        return new DTOUser(
                user.getIdUser(),
                user.getFullName(),
                user.getUsername(),
                user.getBio(),
                user.getUserGender(),
                user.getAvatarUrl(),
                user.getShowWppNumber(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.getLocation()
        );
    }
}
