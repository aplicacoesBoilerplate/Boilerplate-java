package com.java.boilerplate.service.helpers;

import com.java.boilerplate.config.TokensProperties;
import com.java.boilerplate.exception.ExceptionsSystem;
import com.java.boilerplate.repository.IFileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileStorageService implements IFileStorageService {
    private final Path rootLocation;

    public FileStorageService(TokensProperties properties) {
        String dir = properties.getUploadDir() != null ? properties.getUploadDir() : "uploads";
        this.rootLocation = Paths.get(dir).normalize().toAbsolutePath();
    }

    @Override
    public String storeFile(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {
            if (!Files.exists(rootLocation)) {
                Files.createDirectories(rootLocation);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";

            String fileName = prefix + "_" + System.currentTimeMillis() + extension;
            Path destinationFile = rootLocation.resolve(Paths.get(fileName)).normalize().toAbsolutePath();

            if (!destinationFile.getParent().equals(rootLocation.toAbsolutePath())) {
                throw new ExceptionsSystem(
                "It is not possible to store a file outside the current directory.",
                    HttpStatus.BAD_REQUEST
                );
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return "/images/" + fileName;

        } catch (IOException e) {
            throw new ExceptionsSystem("Error saving file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty() || fileUrl.length() <= 10) {
            return;
        }

        try {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            if (!fileName.isEmpty()) {
                Path filePath = rootLocation.resolve(fileName);
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            throw new ExceptionsSystem("Error deleting physical file: " + fileUrl, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
