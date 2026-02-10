package com.java.boilerplate.service;

import com.java.boilerplate.config.TokensProperties;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.Gallery;
import com.java.boilerplate.repository.IGalleryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GalleryService {
    private final IGalleryRepository galleryRepository;
    private final UsersService userService;
    private final TokensProperties tokensProperties;
    private final Path rootLocation;

    public GalleryService(IGalleryRepository galleryRepository, UsersService userService, TokensProperties tokensProperties) {
        this.galleryRepository = galleryRepository;
        this.userService = userService;
        this.tokensProperties = tokensProperties;
        String dir = tokensProperties.getUploadDir() != null ? tokensProperties.getUploadDir() : "uploads";
        this.rootLocation = Paths.get(dir);
    }

    @Transactional
    public List<Gallery> saveAll(List<MultipartFile> files, Long idUser) throws IOException {

        if (!Files.exists(rootLocation)) {
            Files.createDirectories(rootLocation);
        }

        List<Gallery> savedGalleries = new ArrayList<>();

        for (MultipartFile file : files) {
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();

            Path destinationFile = rootLocation.resolve(Paths.get(filename))
                    .normalize().toAbsolutePath();

            if (!destinationFile.getParent().equals(rootLocation.toAbsolutePath())) {
                throw new ExceptionsSystem("It is not possible to store a file outside of the current directory", HttpStatus.BAD_REQUEST);
            }

            try (var inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            Gallery gallery = new Gallery();
            gallery.setIdUser(idUser);
            gallery.setPhotoUrl("/images/" + filename);

            savedGalleries.add(gallery);
        }

        return galleryRepository.saveAll(savedGalleries);
    }

    @Transactional
    public void deletePhotos(List<Gallery> listPhotos) {
        for (Gallery gallery : listPhotos) {
            try {
                galleryRepository.findById(gallery.getIdGallery()).orElseThrow();
                String fileName = gallery.getPhotoUrl().substring(gallery.getPhotoUrl().lastIndexOf("/") + 1);

                Path fileToDelete = rootLocation.resolve(fileName);
                Files.deleteIfExists(fileToDelete);
            } catch (IOException e) {
                throw new ExceptionsSystem(
                        String.format("Error deleting physical file: " + gallery.getPhotoUrl()),
                        HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
        }

        galleryRepository.deleteAll(listPhotos);
    }

    @Transactional(readOnly = true)
    public List<Gallery> findPhotosOfUser(String username) {
        userService.findByUsernameOrEmail(username);
        return galleryRepository.findPhotosOfUser(username);
    }
}
