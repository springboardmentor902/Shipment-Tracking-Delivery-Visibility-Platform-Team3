package com.shiptrack.shiptrack_pro.service.impl;

import com.shiptrack.shiptrack_pro.service.FileUploadService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileUploadServiceImpl implements FileUploadService {

    private final Path uploadDirectory =
            Paths.get("uploads");

    @Override
    public String uploadFile(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        try {
            Files.createDirectories(uploadDirectory);

            String originalName = file.getOriginalFilename();

            String extension = "";

            if (originalName != null && originalName.contains(".")) {
                extension =
                        originalName.substring(
                                originalName.lastIndexOf(".")
                        );
            }

            String fileName =
                    UUID.randomUUID() + extension;

            Path target =
                    uploadDirectory.resolve(fileName);

            Files.copy(
                    file.getInputStream(),
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return "/uploads/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to upload file",
                    e
            );
        }
    }
}