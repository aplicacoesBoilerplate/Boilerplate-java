package com.java.boilerplate.controller.users;

import com.fasterxml.jackson.annotation.JsonView;
import com.java.boilerplate.dto.DTOPagination;
import com.java.boilerplate.dto.users.DTOInsertLocationUser;
import com.java.boilerplate.dto.users.DTORequestWithinRadius;
import com.java.boilerplate.dto.users.DTOUser;
import com.java.boilerplate.dto.users.UserViews;
import com.java.boilerplate.model.pagination.RequestPagination;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
            @PathVariable Long idUser
    ) { return usersHandler.findById(idUser); }

    @PostMapping
    @JsonView(UserViews.Registration.class)
    public ResponseEntity<DTOUser> saveUser(
            @RequestBody @Valid @JsonView(UserViews.Registration.class) DTOUser newUser
    ) { return usersHandler.saveUser(newUser); }

    @PutMapping
    @JsonView(UserViews.Internal.class)
    public ResponseEntity<DTOUser> updateUser(
            @RequestBody @Valid @JsonView(UserViews.Internal.class) DTOUser updateUser
    ) { return usersHandler.updateUser(updateUser); }

    @DeleteMapping
    public ResponseEntity<String> deleteUser() { return usersHandler.deleteUser(); }

    @PostMapping("/pagination")
    @JsonView(UserViews.Public.class)
    public ResponseEntity<DTOPagination<DTOUser>> findPaginationItens(
            @RequestBody RequestPagination request
    ) { return usersHandler.findPaginationItens(request); }

    @PostMapping("/withinRadius")
    @JsonView(UserViews.Public.class)
    public ResponseEntity<DTOPagination<DTOUser>> findWithinRadius(
            @RequestBody DTORequestWithinRadius request
    ) { return usersHandler.findWithinRadius(request); }

    @GetMapping("/find/{usernameOrEmail}")
    @JsonView(UserViews.Internal.class)
    public ResponseEntity<DTOUser> findByUsernameOrEmail(
            @PathVariable String usernameOrEmail
    ) { return  usersHandler.findByUsernameOrEmail(usernameOrEmail); }

    @PutMapping("/location")
    @JsonView(UserViews.Internal.class)
    public ResponseEntity<DTOUser> insertNewUserLocation(
            @RequestBody DTOInsertLocationUser locationUser
    ) { return usersHandler.insertNewUserLocation(locationUser); }

    @PatchMapping(value = "/{username}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DTOUser> updateAvatar(
            @PathVariable String username,
            @RequestParam("file") MultipartFile file
    ) { return usersHandler.updateAvatar(username, file); }

    @PatchMapping("/presence")
    public ResponseEntity<Void> updatePresence(
            @RequestParam String username,
            @RequestParam(required = false, defaultValue = "false") Boolean online
    ) { return usersHandler.updatePresence(username, online); }
}
