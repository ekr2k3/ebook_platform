package com.erk.ebookPlatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;


@Getter
@Setter
@NoArgsConstructor
public class UploadEbookForm {

    @NotBlank
    private String title;

    @NotBlank
    private String author;

    private String description;

    @NotNull
    private MultipartFile file;

    @NotNull
    private String status;

    @NotNull
    private MultipartFile coverUrl;

    @NotNull
    private Boolean previewable;

    @NotNull
    private Integer totalPages;
}
