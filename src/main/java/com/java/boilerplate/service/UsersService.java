package com.java.boilerplate.service;

import com.java.boilerplate.dto.DTOPagination;
import com.java.boilerplate.dto.users.DTOInsertLocationUser;
import com.java.boilerplate.enums.GenderUser;
import com.java.boilerplate.enums.UserRoles;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.model.pagination.RequestPagination;
import com.java.boilerplate.repository.IUsersRepository;
import com.java.boilerplate.service.helpers.LocationService;
import org.locationtech.jts.geom.Point;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsersService {
    private final IUsersRepository usersRepository;
    private final PasswordEncoder encoder;
    private final LocationService locationService;
    private final AuthService authService;

    public UsersService(IUsersRepository usersRepository, PasswordEncoder encoder, LocationService locationService, @Lazy AuthService authService) {
        this.usersRepository = usersRepository;
        this.encoder = encoder;
        this.locationService = locationService;
        this.authService = authService;
    }

    public Users findById(Long idUser) {
        return usersRepository.findById(idUser)
                .orElseThrow(() -> new ExceptionsSystem(
                String.format("User not found for ID: %d", idUser),
                HttpStatus.NOT_FOUND
        ));
    }

    private Boolean userIsAdmin() {
        Users user = authService.getMe();
        return user.getRole().equals(UserRoles.ADMIN);
    }

    private void dataValidModify(Users user, Long idUser) {
        Boolean isAdmin = this.userIsAdmin();

        if (user.getRole().equals(UserRoles.ADMIN) && !isAdmin) {
            throw new ExceptionsSystem(
                    "New administrators can only be created by another administrator",
                    HttpStatus.UNAUTHORIZED
            );
        }

        Users emailExisting = this.findByUsernameOrEmail(user.getEmail());
        Users usernameExisting = this.findByUsernameOrEmail(user.getUserUsername());

        if (idUser != null) {
            if (usernameExisting != null && !usernameExisting.getIdUser().equals(idUser) || emailExisting != null && !emailExisting.getIdUser().equals(idUser)) {
                throw new ExceptionsSystem(
                        "An account with this login information was found. Please log in to access it or recover your password!",
                        HttpStatus.CONFLICT
                );
            }
        } else {
            if (usernameExisting != null || emailExisting != null) {
                throw new ExceptionsSystem(
                        "An account with this login information was found. Please log in to access it or recover your password!",
                        HttpStatus.CONFLICT
                );
            }
        }
    }

    public Users saveUser(Users newUser) {
        this.dataValidModify(newUser, null);
        if (newUser.getPassword() == null || newUser.getPassword().isBlank()) {
            throw new ExceptionsSystem(
                    "The password field is required",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (newUser.getPassword().length() < 8 || newUser.getPassword().length() > 20) {
            throw new ExceptionsSystem(
                    "The password field must be between 8 and 20 characters long",
                    HttpStatus.BAD_REQUEST
            );
        }

        String passwordEncode = encoder.encode(newUser.getPassword());
        newUser.setPassword(passwordEncode);
        return usersRepository.save(newUser);
    }

    public Users updateUser(Users updateUser) {
        Long idUser = authService.getMe().getIdUser();
        Users userDoBanco = this.findById(idUser);

        userDoBanco.setFullName(updateUser.getFullName());
        userDoBanco.setUserUsername(updateUser.getUserUsername());
        userDoBanco.setBio(updateUser.getBio());
        userDoBanco.setUserGender(updateUser.getUserGender());
        userDoBanco.setAvatarUrl(updateUser.getAvatarUrl());
        userDoBanco.setShowWppNumber(updateUser.getShowWppNumber());
        userDoBanco.setPhoneNumber(updateUser.getPhoneNumber());
        userDoBanco.setRole(updateUser.getRole());

        if (updateUser.getEmail() != null) {
            userDoBanco.setEmail(updateUser.getEmail());
        }

        this.dataValidModify(userDoBanco, idUser);
        return usersRepository.save(userDoBanco);
    }

    public void deleteUser() {
        usersRepository.delete(authService.getMe());
    }

    public DTOPagination<Users> findPaginationItens(RequestPagination request) {
        return usersRepository.findPaginationItens(request, "idUser");
    }

    public List<Users> findWithinRadius(Point point, Long radius) {
        GenderUser userGender = authService.getMe().getUserGender();
        return usersRepository.findWithinRadius(point, radius, userGender);
    }

    public Users findByUsernameOrEmail(String usernameOrEmail) {
        return usersRepository.findByUsernameOrEmail(usernameOrEmail);
    }

    public Users insertNewUserLocation(DTOInsertLocationUser locationUser) {
        Users user = this.findById(locationUser.idUser());
        Point point = locationService.createPoint(locationUser.location());
        user.setLocation(point);
        usersRepository.save(user);
        return user;
    }
}
