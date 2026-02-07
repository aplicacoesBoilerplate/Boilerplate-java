package com.java.boilerplate.controller.users;

import com.java.boilerplate.dto.DTOPagination;
import com.java.boilerplate.dto.users.DTOInsertLocationUser;
import com.java.boilerplate.dto.users.DTORequestWithinRadius;
import com.java.boilerplate.dto.users.DTOUser;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.model.pagination.RequestPagination;
import com.java.boilerplate.service.UsersService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class UsersHandler {
    private final UsersService usersService;

    public UsersHandler(UsersService usersService) {
        this.usersService = usersService;
    }

    public ResponseEntity<DTOUser> findById(Long idUser, String authorization) {
        Users user = usersService.findById(idUser, authorization);
        return ResponseEntity.ok(DTOUser.fromEntity(user));
    }

    public ResponseEntity<DTOUser> saveUser(DTOUser newUser, String authorization) {
        Users user = usersService.saveUser(newUser.toEntity(), authorization);
        return ResponseEntity.ok(DTOUser.fromEntity(user));
    }

    public ResponseEntity<DTOUser> updateUser(DTOUser updateUser, Long idUser, String authorization) {
        Users user = usersService.updateUser(updateUser.toEntity(), idUser, authorization);
        return ResponseEntity.ok(DTOUser.fromEntity(user));
    }

    public ResponseEntity<String> deleteUser(Long idUser, String authorization) {
        String messageResponse = String.format("User %d successfully removed!", idUser);
        usersService.deleteUser(idUser, authorization);
        return ResponseEntity.ok(messageResponse);
    }

    public ResponseEntity<DTOPagination<DTOUser>> findPaginationItens(RequestPagination request, String authorization) {
        DTOPagination<Users> paginationUsers = usersService.findPaginationItens(request, authorization);
        DTOPagination<DTOUser> response = paginationUsers.map(DTOUser::fromEntity);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<DTOPagination<DTOUser>> findWithinRadius(DTORequestWithinRadius request, String authorization) {
        DTOPagination<Users> usersInRadius = usersService.findWithinRadius(request.point(), request.radius(), authorization);
        DTOPagination<DTOUser> response = usersInRadius.map(DTOUser::fromEntity);
        return ResponseEntity.ok(response);
    }

    public ResponseEntity<DTOUser> findByUsernameOrEmail(String usernameOrEmail, String authorization) {
        Object principal = usersService.findByUsernameOrEmail(usernameOrEmail, authorization);

        if (principal instanceof Users user)
            return ResponseEntity.ok(DTOUser.fromEntity(user));

        throw new ExceptionsSystem("Converted identity is not a valid User", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public ResponseEntity<DTOUser> insertNewUserLocation(DTOInsertLocationUser locationUser, String authorization) {
        Users user = usersService.insertNewUserLocation(locationUser, authorization);
        return ResponseEntity.ok(DTOUser.fromEntity(user));
    }
}
