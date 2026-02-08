package com.java.boilerplate.controller.gallery;

import com.java.boilerplate.model.Gallery;
import com.java.boilerplate.service.GalleryService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GalleryHandler {
    private final GalleryService galleryService;

    public GalleryHandler(GalleryService galleryService) {
        this.galleryService = galleryService;
    }

    public ResponseEntity<List<Gallery>> saveAll(List<Gallery> galleries, Long userId) {
        return ResponseEntity.ok(galleryService.saveAll(galleries, userId));
    }

    public ResponseEntity<String> deletePhotos(List<Gallery> listPhotos) {
        galleryService.deletePhotos(listPhotos);
        return ResponseEntity.ok().body(String.format("%d photos successfully removed!", listPhotos.size()));
    }

    public ResponseEntity<List<Gallery>> findPhotosOfUser(String username) {
        List<Gallery> photos = galleryService.findPhotosOfUser(username);
        return ResponseEntity.ok(photos);
    }
}
