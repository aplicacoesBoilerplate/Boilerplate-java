package com.java.boilerplate.controller.users;

import com.java.boilerplate.dto.DTOPagination;
import com.java.boilerplate.dto.users.DTOInsertLocationUser;
import com.java.boilerplate.dto.users.DTORequestWithinRadius;
import com.java.boilerplate.dto.users.DTOUser;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.model.pagination.RequestPagination;
import com.java.boilerplate.service.UsersService;
import com.java.boilerplate.service.helpers.LocationService;
import org.locationtech.jts.geom.Point;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UsersHandler {
    private final UsersService usersService;
    private final LocationService locationService;

    public UsersHandler(UsersService usersService, LocationService locationService) {
        this.usersService = usersService;
        this.locationService = locationService;
    }

    public ResponseEntity<DTOUser> findById(Long idUser) {
        Users user = usersService.findById(idUser);
        return ResponseEntity.ok(DTOUser.fromEntity(user));
    }

    public ResponseEntity<DTOUser> saveUser(DTOUser newUser) {
        Users user = usersService.saveUser(newUser.toEntity());
        return ResponseEntity.ok(DTOUser.fromEntity(user));
    }

    public ResponseEntity<DTOUser> updateUser(DTOUser updateUser) {
        Users user = usersService.updateUser(updateUser.toEntity());
        return ResponseEntity.ok(DTOUser.fromEntity(user));
    }

    public ResponseEntity<String> deleteUser() {
        usersService.deleteUser();
        return ResponseEntity.ok().body("User successfully removed!");
    }

    public ResponseEntity<DTOPagination<DTOUser>> findPaginationItens(RequestPagination request) {
        DTOPagination<Users> paginationUsers = usersService.findPaginationItens(request);
        DTOPagination<DTOUser> response = paginationUsers.map(DTOUser::fromEntity);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<DTOPagination<DTOUser>> findWithinRadius(DTORequestWithinRadius request) {
        Point point = locationService.createPoint(request.location());
        List<Users> usersInRadius = usersService.findWithinRadius(point, request.radius());
        List<DTOUser> dtoUserList = usersInRadius.stream().map(DTOUser::fromEntity).toList();;
        DTOPagination<DTOUser> response = new DTOPagination<>(
                dtoUserList.size(),
                0,
                dtoUserList.size(),
                false,
                dtoUserList
        );
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<DTOUser> findByUsernameOrEmail(String usernameOrEmail) {
        Object principal = usersService.findByUsernameOrEmail(usernameOrEmail);

        if (principal instanceof Users user)
            return ResponseEntity.ok(DTOUser.fromEntity(user));

        throw new ExceptionsSystem("Converted identity is not a valid User", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public ResponseEntity<DTOUser> insertNewUserLocation(DTOInsertLocationUser locationUser) {
        Users user = usersService.insertNewUserLocation(locationUser);
        return ResponseEntity.ok(DTOUser.fromEntity(user));
    }
}
