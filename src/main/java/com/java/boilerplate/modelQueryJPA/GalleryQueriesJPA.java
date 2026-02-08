package com.java.boilerplate.modelQueryJPA;

import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.NamedQueries;
import org.hibernate.annotations.NamedQuery;

@MappedSuperclass
@NamedQueries({
    @NamedQuery(name = "Gallery.findPhotosOfUser", query = GalleryQueriesJPA.sqlFindPhotosOfUser),
    @NamedQuery(name = "Gallery.countPhotosOfUser", query = GalleryQueriesJPA.sqlCountPhotosOfUser)
})
public class GalleryQueriesJPA {
    static final String sqlFindPhotosOfUser = """
        SELECT p FROM Gallery p
        WHERE p.user.userUsername = :username
        """;

    static final String sqlCountPhotosOfUser = """
        SELECT COUNT(p) FROM Gallery p
        WHERE p.user.id = :userId
        """;
}