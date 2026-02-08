package com.java.boilerplate.repository;

import com.java.boilerplate.model.Gallery;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IGalleryRepository extends IBaseRepository<Gallery> {
    List<Gallery> findPhotosOfUser(@Param("username") String username);
    long countPhotosOfUser(@Param("userId") Long userId);
}
