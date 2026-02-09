package com.java.boilerplate.controller.gallery;

import com.java.boilerplate.model.Gallery;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/gallery")
public class GalleryController {
    private final GalleryHandler galleryHandler;

    public GalleryController(GalleryHandler galleryHandler) {
        this.galleryHandler = galleryHandler;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<Gallery>> saveAll(
            @RequestParam("files") List<MultipartFile> galleries,
            @RequestParam Long idUser
    ) throws IOException { return galleryHandler.saveAll(galleries, idUser); }

    @DeleteMapping
    public ResponseEntity<String> deleteAll(
            @RequestBody List<Gallery> listPhotos
    ) { return galleryHandler.deletePhotos(listPhotos); }

    @GetMapping
    public ResponseEntity<List<Gallery>> findPhotosOfUser(
            @RequestParam String username
    ) { return galleryHandler.findPhotosOfUser(username); }
}
