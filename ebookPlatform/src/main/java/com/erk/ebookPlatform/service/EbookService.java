package com.erk.ebookPlatform.service;

import com.erk.ebookPlatform.dto.UploadEbookForm;
import com.erk.ebookPlatform.entity.Ebook;
import com.erk.ebookPlatform.entity.User;
import com.erk.ebookPlatform.enums.EbookStatus;
import com.erk.ebookPlatform.enums.Role;
import com.erk.ebookPlatform.repository.EbookRepository;
import com.erk.ebookPlatform.repository.UserRepository;
import com.erk.ebookPlatform.service.storage.FileStorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class EbookService {

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50 MB

    private final EbookRepository ebookRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public Ebook uploadEbook(UploadEbookForm form) {
        User seller = userRepository.findById(form.getSellerId())
                .orElseThrow(() -> new IllegalArgumentException("Seller not found"));

        if (seller.getRole() != Role.SELLER) {
            throw new IllegalArgumentException("Only users with SELLER role can upload ebooks");
        }

        var file = form.getFile();
        validateFile(file);

        String storedPath = fileStorageService.store(file);

        Ebook ebook = new Ebook();
        ebook.setTitle(form.getTitle());
        ebook.setAuthor(form.getAuthor());
        ebook.setDescription(form.getDescription());
        ebook.setFormat(form.getFormat());
        ebook.setSeller(seller);
        ebook.setFileName(StringUtils.cleanPath(file.getOriginalFilename()));
        ebook.setFileUrl(storedPath);
        ebook.setDriveFileId(storedPath);
        ebook.setFileSize(file.getSize());
        ebook.setPreviewable(true);
        ebook.setStatus(EbookStatus.DRAFT);

        return ebookRepository.save(ebook);
    }

    private void validateFile(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Please upload a PDF or EPUB file");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File exceeds maximum allowed size of 50 MB");
        }

        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getExtension(filename).toLowerCase();
        if (!extension.equals("pdf") && !extension.equals("epub")) {
            throw new IllegalArgumentException("Only PDF and EPUB formats are accepted");
        }
    }

    private String getExtension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index == -1) {
            return "";
        }
        return filename.substring(index + 1);
    }
}
