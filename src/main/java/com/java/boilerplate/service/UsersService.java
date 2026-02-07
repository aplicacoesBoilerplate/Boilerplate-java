package com.java.boilerplate.service;

import com.java.boilerplate.dto.DTOPagination;
import com.java.boilerplate.dto.users.DTOInsertLocationUser;
import com.java.boilerplate.enums.UserRoles;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.model.pagination.RequestPagination;
import com.java.boilerplate.repository.IUsersRepository;
import com.java.boilerplate.service.helpers.LocationService;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsersService {

    private final IUsersRepository usersRepository;
    private final PasswordEncoder encoder;
    private final LocationService locationService;

    public UsersService(IUsersRepository usersRepository, PasswordEncoder encoder, LocationService locationService) {
        this.usersRepository = usersRepository;
        this.encoder = encoder;
        this.locationService = locationService;
    }

    public Users findById(Long idUser, String authorization) {
        return usersRepository.findById(idUser)
                .orElseThrow(() -> new ExceptionsSystem(
                String.format("User not found for ID: %d", idUser),
                HttpStatus.NOT_FOUND
        ));
    }

    public Users saveUser(Users newUser, String authorization) {
        Users userEmail = usersRepository.findByUsernameOrEmail(newUser.getEmail());
        Users userUsername = usersRepository.findByUsernameOrEmail(newUser.getUserUsername());

        if (userEmail != null || userUsername != null) {
            throw new ExceptionsSystem(
                    "An account with this login information was found. Please log in to access it or recover your password!",
                    HttpStatus.CONFLICT
            );
        }

        String passwordEncode = encoder.encode(newUser.getPassword());
        newUser.setPassword(passwordEncode);
        newUser.setRole(UserRoles.ADMIN);
        return usersRepository.save(newUser);
    }

    public Users updateUser(Users updateUser, Long idUser, String authorization) {
        Users userExisting = this.findById(idUser, authorization);
        Users usernameExisting = (Users) this.findByUsernameOrEmail(updateUser.getUserUsername(), authorization);

        if (usernameExisting != null && !usernameExisting.getIdUser().equals(idUser)) {
            throw new ExceptionsSystem(
                    String.format("Username %s is not available", updateUser.getUserUsername()),
                    HttpStatus.CONFLICT
            );
        } else {
            userExisting.setUserUsername(updateUser.getUserUsername());
        }

        userExisting.setFullName(updateUser.getFullName());
        userExisting.setBio(updateUser.getBio());
        userExisting.setAvatarUrl(updateUser.getAvatarUrl());
        userExisting.setShowWppNumber(updateUser.getShowWppNumber());
        userExisting.setPhoneNumber(updateUser.getPhoneNumber());
        return usersRepository.save(userExisting);
    }

    public void deleteUser(Long idUser, String authorization) {
        Users user = this.findById(idUser, authorization);
        usersRepository.delete(user);
    }

    public DTOPagination<Users> findPaginationItens(RequestPagination request, String authorization) {
        return usersRepository.findPaginationItens(request, "idUser");
    }

    public DTOPagination<Users> findWithinRadius(Point point, Long radius, String authorization) {
        return usersRepository.findWithinRadius(point, radius);
    }

    public Users findByUsernameOrEmail(String usernameOrEmail, String authorization) {
        return usersRepository.findByUsernameOrEmail(usernameOrEmail);
    }

    public Users insertNewUserLocation(DTOInsertLocationUser locationUser, String authorization) {
        Users user = this.findById(locationUser.idUser(), authorization);

        Point point = locationService.createPoint(locationUser.location());

        user.setLocation(point);
        usersRepository.save(user);
        return this.findById(locationUser.idUser(), authorization);
    }
}
