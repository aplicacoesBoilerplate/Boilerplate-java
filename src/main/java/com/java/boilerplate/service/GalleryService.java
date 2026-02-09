package com.java.boilerplate.service;

import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.model.Gallery;
import com.java.boilerplate.repository.IGalleryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GalleryService {
    private final IGalleryRepository galleryRepository;
    private final UsersService userService;

    public GalleryService(IGalleryRepository galleryRepository, UsersService userService) {
        this.galleryRepository = galleryRepository;
        this.userService = userService;
    }

    @Transactional
    public List<Gallery> saveAll(List<Gallery> galleries, Long userId) {
        long fotosExistentes = galleryRepository.countPhotosOfUser(userId);
        long novasFotos = galleries.stream()
                .filter(g -> g.getIdGallery() == null)
                .count();

        if (fotosExistentes + novasFotos > 10) {
            throw new ExceptionsSystem(
                    "Limit exceeded. You can only have 10 photos maximum",
                    (HttpStatus) HttpStatusCode.valueOf(422)
            );
        }

        galleries.forEach(gallery -> {
            gallery.setIdUser(userId);
        });

        return galleryRepository.saveAll(galleries);
    }

    @Transactional
    public void deletePhotos(List<Gallery> listPhotos) {
        galleryRepository.deleteAll(listPhotos);
    }

    @Transactional(readOnly = true)
    public List<Gallery> findPhotosOfUser(String username) {
        userService.findByUsernameOrEmail(username);
        return galleryRepository.findPhotosOfUser(username);
    }
}
