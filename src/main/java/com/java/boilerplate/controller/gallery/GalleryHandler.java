package com.java.boilerplate.controller.gallery;

import com.java.boilerplate.model.Gallery;
import com.java.boilerplate.service.GalleryService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Component
public class GalleryHandler {
    private final GalleryService galleryService;

    public GalleryHandler(GalleryService galleryService) {
        this.galleryService = galleryService;
    }

    public ResponseEntity<List<Gallery>> saveAll(List<MultipartFile> galleries, Long userId) throws IOException {
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
