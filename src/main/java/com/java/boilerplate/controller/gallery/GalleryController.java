package com.java.boilerplate.controller.gallery;

import com.java.boilerplate.model.Gallery;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/gallery")
public class GalleryController {
    private final GalleryHandler galleryHandler;

    public GalleryController(GalleryHandler galleryHandler) {
        this.galleryHandler = galleryHandler;
    }

    @PostMapping
    public ResponseEntity<List<Gallery>> saveAll(
            @RequestBody List<Gallery> listPhotos,
            @RequestParam Long idUser
    ) { return galleryHandler.saveAll(listPhotos, idUser); }

    @DeleteMapping
    public ResponseEntity<String> deleteAll(
            @RequestBody List<Gallery> listPhotos
    ) { return galleryHandler.deletePhotos(listPhotos); }

    @GetMapping
    public ResponseEntity<List<Gallery>> findPhotosOfUser(
            @RequestParam String username
    ) { return galleryHandler.findPhotosOfUser(username); }
}
