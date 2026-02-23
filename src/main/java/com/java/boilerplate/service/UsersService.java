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
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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

    @Transactional(readOnly = true)
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

    private void dataValidModify(Users dataUser, Long idUser) {
        Boolean authenticatedUserIsAdmin = this.userIsAdmin();

        if (dataUser.getRole().equals(UserRoles.ADMIN) && !authenticatedUserIsAdmin) {
            throw new ExceptionsSystem(
                    "New administrators can only be created by another administrator",
                    HttpStatus.UNAUTHORIZED
            );
        }

        Users emailExisting = this.findByUsernameOrEmail(dataUser.getEmail());
        Users usernameExisting = this.findByUsernameOrEmail(dataUser.getUserUsername());

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

    @Transactional
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
        newUser.setIsOnline(true);
        newUser.setIsActive(true);
        return usersRepository.save(newUser);
    }

    @Transactional
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

    @Transactional
    public void deleteUser() {
        Users user = authService.getMe();

        String deletedTag = "deleted_" + System.currentTimeMillis() + user.getUserUsername();

        user.setFullName(String.format("Usuário %s excluído", user.getUserUsername()));
        user.setUserUsername(deletedTag);
        user.setEmail(deletedTag + "@deleted.com");
        user.setBio(null);
        user.setAvatarUrl(null);
        user.setShowWppNumber(false);
        user.setPhoneNumber(null);
        user.setIsOnline(false);
        user.setIsActive(false);
        usersRepository.save(user);
    }

    @Transactional(readOnly = true)
    public DTOPagination<Users> findPaginationItens(RequestPagination request) {
        return usersRepository.findPaginationItens(request, "idUser");
    }

    @Transactional(readOnly = true)
    public List<Users> findWithinRadius(Point point, Long radius) {
        Users user = authService.getMe();
        return usersRepository.findWithinRadius(point, radius, user.getUserGender(), user.getIdUser());
    }

    @Transactional(readOnly = true)
    public Users findByUsernameOrEmail(String usernameOrEmail) {
        Users user = usersRepository.findByUsernameOrEmail(usernameOrEmail);
        if (user == null) {
            throw new ExceptionsSystem(
                    "User not found",
                    HttpStatus.NOT_FOUND
            );
        }
        return user;
    }

    @Transactional
    public Users insertNewUserLocation(DTOInsertLocationUser locationUser) {
        Users user = this.findById(locationUser.idUser());
        Point point = locationService.createPoint(locationUser.location());
        user.setLocation(point);
        usersRepository.save(user);
        return user;
    }

    @Transactional
    public Users updateAvatar(String username, MultipartFile file) {
        Users user = this.findByUsernameOrEmail(username);

        try {
            Path uploadPath = Paths.get("/app/uploads");

            if (user.getAvatarUrl() != null) {
                String oldFileName = user.getAvatarUrl().substring(user.getAvatarUrl().lastIndexOf("/") + 1);
                Path oldFilePath = uploadPath.resolve(oldFileName);
                Files.deleteIfExists(oldFilePath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String fileName = "avatar_" + username + "_" + System.currentTimeMillis() + extension;

            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            try (InputStream inputStream = file.getInputStream()) {
                Path filePath = uploadPath.resolve(fileName);
                Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            }

            user.setAvatarUrl("/images/" + fileName);
            return usersRepository.save(user);
        } catch (IOException e) {
            throw new RuntimeException(
                    String.format("Error saving avatar: %s", e.getMessage()),
                    e
            );
        }
    }

    @Transactional
    public void updatePresence(String username, boolean online) {
        Users user = usersRepository.findByUsernameOrEmail(username);
        if (user != null) {
            user.setIsOnline(online);
            usersRepository.save(user);
        }
    }
}
