package com.java.boilerplate.controller.users;

import com.fasterxml.jackson.annotation.JsonView;
import com.java.boilerplate.dto.DTOPagination;
import com.java.boilerplate.dto.users.DTOInsertLocationUser;
import com.java.boilerplate.dto.users.DTORequestWithinRadius;
import com.java.boilerplate.dto.users.DTOUser;
import com.java.boilerplate.dto.users.UserViews;
import com.java.boilerplate.model.pagination.RequestPagination;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UsersController {
    private final UsersHandler usersHandler;

    public UsersController(UsersHandler usersHandler) {
        this.usersHandler = usersHandler;
    }

    @GetMapping("/{idUser}")
    @JsonView(UserViews.Public.class)
    public ResponseEntity<DTOUser> findById(
            @PathVariable Long idUser,
            @RequestHeader("Authorization") String authorization
    ) { return usersHandler.findById(idUser, authorization); }

    @PostMapping
    @JsonView(UserViews.Registration.class)
    public ResponseEntity<DTOUser> saveUser(
            @RequestBody @Valid @JsonView(UserViews.Registration.class) DTOUser newUser,
            @RequestHeader("Authorization") String authorization
    ) { return usersHandler.saveUser(newUser, authorization); }

    @PutMapping("/{idUser}")
    @JsonView(UserViews.Internal.class)
    public ResponseEntity<DTOUser> updateUser(
            @RequestBody @Valid @JsonView(UserViews.Internal.class) DTOUser updateUser,
            @PathVariable Long idUser,
            @RequestHeader("Authorization") String authorization
    ) { return usersHandler.updateUser(updateUser, idUser, authorization); }

    @DeleteMapping("/{idUser}")
    public ResponseEntity<String> deleteUser(
             @PathVariable Long idUser,
             @RequestHeader("Authorization") String authorization
    ) { return usersHandler.deleteUser(idUser, authorization); }

    @PostMapping("/pagination")
    @JsonView(UserViews.Public.class)
    public ResponseEntity<DTOPagination<DTOUser>> findPaginationItens(
            @RequestBody RequestPagination request,
            @RequestHeader("Authorization") String authorization
    ) { return usersHandler.findPaginationItens(request, authorization); }

    @PostMapping("/withinRadius")
    @JsonView(UserViews.Public.class)
    public ResponseEntity<DTOPagination<DTOUser>> findWithinRadius(
            @RequestBody DTORequestWithinRadius request,
            @RequestHeader("Authorization") String authorization
    ) { return usersHandler.findWithinRadius(request, authorization); }

    @GetMapping("/find/{usernameOrEmail}")
    @JsonView(UserViews.Internal.class)
    public ResponseEntity<DTOUser> findByUsernameOrEmail(
            @PathVariable String usernameOrEmail,
            @RequestHeader("Authorization") String authorization
    ) { return  usersHandler.findByUsernameOrEmail(usernameOrEmail, authorization); }

    @PutMapping("/location")
    @JsonView(UserViews.Internal.class)
    public ResponseEntity<DTOUser> insertNewUserLocation(
            @RequestBody DTOInsertLocationUser locationUser,
            @RequestHeader("Authorization") String authorization
    ) { return usersHandler.insertNewUserLocation(locationUser, authorization); }
}
