package com.java.boilerplate.model;

import com.java.boilerplate.modelQueryJPA.GalleryQueriesJPA;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "gallery_photos")
@Data
@EqualsAndHashCode(callSuper = true)
public class Gallery extends GalleryQueriesJPA {
    @Id
    @Column(name = "id_gallery")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idGallery;

    @Column(name = "gallery_photo_url")
    private String photoUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_user", insertable = false, updatable = false)
    private Users user;
}
