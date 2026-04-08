package com.erk.ebookPlatform.service;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import com.erk.ebookPlatform.dto.UploadEbookForm;
import com.erk.ebookPlatform.entity.Ebook;
import com.erk.ebookPlatform.entity.User;
import com.erk.ebookPlatform.enums.EbookFormat;
import com.erk.ebookPlatform.enums.EbookStatus;
import com.erk.ebookPlatform.enums.SourceType;
import com.erk.ebookPlatform.repository.EbookRepository;
import com.erk.ebookPlatform.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;



@Service
public class EbookService {

    private final CloudinaryService cloudinaryService;
    private final EbookRepository ebookRepository;
    private final UserRepository userRepository;

    public EbookService(CloudinaryService cloudinaryService, // dùng constructor để inject or dùng @Autowired
                        EbookRepository ebookRepository,
                        UserRepository userRepository) {
        this.cloudinaryService = cloudinaryService;
        this.ebookRepository = ebookRepository;
        this.userRepository = userRepository;
    }

    public void uploadEbook(UploadEbookForm form) {
        MultipartFile file = form.getFile();
        MultipartFile coverFile = form.getCoverUrl();

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File ebook không được để trống");
        }

        if (coverFile == null || coverFile.isEmpty()) {
            throw new IllegalArgumentException("Ảnh bìa không được để trống");
        }

        User seller = userRepository.findByEmail("seller01@example.com")
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy seller"));

        try {
            Map<String, Object> uploadResult = cloudinaryService.uploadFile(file);
            Map<String, Object> coverUploadResult = cloudinaryService.uploadFile(coverFile);

            Ebook ebook = new Ebook();
            ebook.setSeller(seller); 


            ebook.setTitle(form.getTitle());
            ebook.setAuthor(form.getAuthor());
            ebook.setDescription(form.getDescription());
            ebook.setTotalPages(form.getTotalPages());
            ebook.setPreviewable(Boolean.TRUE.equals(form.getPreviewable()));

            ebook.setStatus(EbookStatus.valueOf(form.getStatus().toUpperCase()));
            ebook.setFormat(EbookFormat.valueOf(((String) uploadResult.get("format")).toUpperCase()));

            Object bytesObj = uploadResult.get("bytes");
            if (bytesObj instanceof Number number) {
                ebook.setFileSize(number.longValue());
            }

            ebook.setFileName(file.getOriginalFilename());
            ebook.setFileUrl((String) uploadResult.get("secure_url"));
            ebook.setCoverUrl((String) coverUploadResult.get("secure_url"));

            ebookRepository.save(ebook);

            System.out.println("Ebook URL: " + ebook.getFileUrl() + " - logs from EbookService");
        } catch (IOException e) {
            System.err.println("Lỗi upload file lên Cloudinary: " + e.getMessage());
            throw new RuntimeException("Failed to upload ebook", e);
        } catch (IllegalArgumentException e) {
            System.err.println("Dữ liệu không hợp lệ: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.err.println("Lỗi không xác định khi upload ebook: " + e.getMessage());
            throw new RuntimeException("Unexpected error while uploading ebook", e);
        }
    }
}
