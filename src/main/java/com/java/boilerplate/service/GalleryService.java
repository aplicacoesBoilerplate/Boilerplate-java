package com.java.boilerplate.service;

import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.Gallery;
import com.java.boilerplate.repository.IFileStorageService;
import com.java.boilerplate.repository.IGalleryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GalleryService {
    private final IGalleryRepository galleryRepository;
    private final UsersService userService;
    private final IFileStorageService fileStorageService;

    public GalleryService(IGalleryRepository galleryRepository, UsersService userService, IFileStorageService fileStorageService) {
        this.galleryRepository = galleryRepository;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public List<Gallery> saveAll(List<MultipartFile> files, Long idUser) {
        List<Gallery> savedGalleries = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file != null && !file.isEmpty()) {
                String prefix = String.format("gallery_%s", UUID.randomUUID());
                String fileUrl = fileStorageService.storeFile(file, prefix);

                Gallery gallery = new Gallery();
                gallery.setIdUser(idUser);
                gallery.setPhotoUrl(fileUrl);

                savedGalleries.add(gallery);
            }
        }

        return galleryRepository.saveAll(savedGalleries);
    }

    @Transactional
    public void deletePhotos(List<Gallery> listPhotos) {
        for (Gallery gallery : listPhotos) {
            galleryRepository.findById(gallery.getIdGallery())
                .orElseThrow(() -> new ExceptionsSystem(
                        "Photo not found",
                        HttpStatus.NOT_FOUND
                ));

            if (gallery.getPhotoUrl() != null) {
                fileStorageService.deleteFile(gallery.getPhotoUrl());
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
