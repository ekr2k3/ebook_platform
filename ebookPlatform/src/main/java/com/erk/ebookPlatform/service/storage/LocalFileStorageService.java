package com.erk.ebookPlatform.service.storage;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path uploadRoot;

    public LocalFileStorageService() {
        this.uploadRoot = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException e) {
            throw new StorageException("Unable to initialize local storage", e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        if (originalFilename.isBlank()) {
            throw new StorageException("Uploaded file must have a name");
        }

        String safeName = UUID.randomUUID() + "-" + originalFilename;
        Path target = uploadRoot.resolve(safeName);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageException("Failed to store file " + originalFilename, e);
        }

        return target.toString();
    }
}
