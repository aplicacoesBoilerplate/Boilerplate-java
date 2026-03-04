package com.java.boilerplate.service;

import com.java.boilerplate.dto.DTOPagination;
import com.java.boilerplate.dto.users.DTOInsertLocationUser;
import com.java.boilerplate.enums.GenderUser;
import com.java.boilerplate.enums.UserRoles;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.Users;
import com.java.boilerplate.model.pagination.RequestPagination;
import com.java.boilerplate.repository.IFileStorageService;
import com.java.boilerplate.repository.IUsersRepository;
import com.java.boilerplate.service.helpers.HashUtil;
import com.java.boilerplate.service.helpers.LocationService;
import org.locationtech.jts.geom.Point;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

@Service
public class UsersService {
    private final IUsersRepository usersRepository;
    private final PasswordEncoder encoder;
    private final LocationService locationService;
    private final AuthService authService;
    private final IFileStorageService fileStorageService;

    public UsersService(IUsersRepository usersRepository, PasswordEncoder encoder, LocationService locationService, @Lazy AuthService authService, IFileStorageService fileStorageService) {
        this.usersRepository = usersRepository;
        this.encoder = encoder;
        this.locationService = locationService;
        this.authService = authService;
        this.fileStorageService = fileStorageService;
    }

    private Boolean userIsAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || Objects.equals(auth.getPrincipal(), "anonymousUser")) {
            return false;
        }

        try {
            Users user = authService.getMe();
            return user.getRole().equals(UserRoles.ADMIN);
        } catch (Exception e) {
            return false;
        }
    }

    private void dataValidModify(Users dataUserTransaction, Long idUser) {
        Boolean authenticatedUserIsAdmin = this.userIsAdmin();

        if (dataUserTransaction.getRole().equals(UserRoles.ADMIN) && !authenticatedUserIsAdmin) {
            throw new ExceptionsSystem(
                    "New administrators can only be created by another administrator",
                    HttpStatus.UNAUTHORIZED
            );
        }

        Users emailExisting = usersRepository.findByUsernameOrEmail(dataUserTransaction.getEmail());
        Users usernameExisting = usersRepository.findByUsernameOrEmail(dataUserTransaction.getUserUsername());

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

    @Transactional(readOnly = true)
    public Users findById(Long idUser) {
        return usersRepository.findById(idUser)
                .orElseThrow(() -> new ExceptionsSystem(
                        String.format("User not found for ID: %d", idUser),
                        HttpStatus.NOT_FOUND
                ));
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

        if (newUser.getAvatarUrl() == null || newUser.getAvatarUrl().isBlank()) {
            String defaultAvatar = newUser.getUserGender() == GenderUser.MALE
                    ? "/default-avatar-male.png"
                    : "/default-avatar-female.png";
            newUser.setAvatarUrl(defaultAvatar);
        }

        if (newUser.getEmail() != null && newUser.getEmailHash() == null) {
            newUser.setEmailHash(HashUtil.generateSha256(newUser.getEmail()));
        }

        String passwordEncode = encoder.encode(newUser.getPassword());
        newUser.setPassword(passwordEncode);
        newUser.setIsOnline(true);

        if (newUser.getIsActive() == null) {
            newUser.setIsActive(true);
        }

        return usersRepository.save(newUser);
    }

    @Transactional
    public Users updateUser(Users updateUser) {
        Long idUser = authService.getMe().getIdUser();
        Users userInDB = this.findById(idUser);

        userInDB.setFullName(updateUser.getFullName());
        userInDB.setUserUsername(updateUser.getUserUsername());
        userInDB.setBio(updateUser.getBio());
        userInDB.setUserGender(updateUser.getUserGender());
        userInDB.setAvatarUrl(updateUser.getAvatarUrl());
        userInDB.setShowWppNumber(updateUser.getShowWppNumber());
        userInDB.setPhoneNumber(updateUser.getPhoneNumber());
        userInDB.setRole(updateUser.getRole());

        if (updateUser.getEmail() != null) {
            userInDB.setEmail(updateUser.getEmail());
        }

        this.dataValidModify(userInDB, idUser);
        return usersRepository.save(userInDB);
    }

    @Transactional
    public void deleteUser() {
        Users user = authService.getMe();

        String deletedTag = "deleted_" + System.currentTimeMillis();

        user.setFullName("User Deleted");
        user.setUserUsername(deletedTag);
        user.setEmail(deletedTag + "@deleted.com");

        user.setShowWppNumber(false);
        user.setPhoneNumber(null);
        user.setBio(null);
        user.setAvatarUrl(null);
        user.setIsOnline(false);
        user.setIsActive(false);

        if (user.getAvatarUrl() == null || user.getAvatarUrl().isBlank()) {
            String defaultAvatar = user.getUserGender() == GenderUser.MALE
                    ? "/default-avatar-male.png"
                    : "/default-avatar-female.png";
            user.setAvatarUrl(defaultAvatar);
        }

        usersRepository.save(user);
    }

    @Transactional
    public Users saveEntity(Users user) {
        return usersRepository.save(user);
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

        if (file != null && !file.isEmpty()) {
            fileStorageService.deleteFile(user.getAvatarUrl());
            String fileUrl = fileStorageService.storeFile(file, "avatar_" + username);
            user.setAvatarUrl(fileUrl);
        }

        return usersRepository.save(user);
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
