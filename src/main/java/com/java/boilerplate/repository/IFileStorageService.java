package com.java.boilerplate.repository;

import org.springframework.web.multipart.MultipartFile;

public interface IFileStorageService {
    String storeFile(MultipartFile file, String prefix);
    void deleteFile(String fileUrl);
}