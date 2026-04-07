package com.erk.ebookPlatform.service.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    /**
     * Store the supplied file and return a storage identifier that can be persisted in the database.
     */
    String store(MultipartFile file);
}
