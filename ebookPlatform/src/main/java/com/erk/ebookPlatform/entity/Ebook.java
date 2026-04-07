package com.erk.ebookPlatform.entity;

import com.erk.ebookPlatform.enums.EbookFormat;
import com.erk.ebookPlatform.enums.EbookStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ebooks")
@Getter
@Setter
@NoArgsConstructor
public class Ebook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 255)
    private String author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EbookFormat format;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "drive_file_id", length = 255)
    private String driveFileId;

    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "total_pages")
    private Integer totalPages;

    @Column(nullable = false)
    private Boolean previewable = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EbookStatus status = EbookStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "ebook")
    private List<EbookValidationLog> validationLogs = new ArrayList<>();

    @OneToMany(mappedBy = "ebook")
    private List<UserEbook> userEbooks = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
