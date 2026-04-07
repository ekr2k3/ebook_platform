package com.erk.ebookPlatform.controller;

import com.erk.ebookPlatform.dto.UploadEbookForm;
import com.erk.ebookPlatform.enums.EbookFormat;
import com.erk.ebookPlatform.service.EbookService;
import com.erk.ebookPlatform.service.storage.StorageException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles seller-facing pages related to ebook upload for Sprint 1 (C1-02).
 */
@Controller
@RequestMapping("/seller/ebooks")
@RequiredArgsConstructor
public class EbookController {

    private static final String VIEW_UPLOAD_FORM = "seller/upload";

    private final EbookService ebookService;

    @ModelAttribute("formats")
    public EbookFormat[] availableFormats() {
        return EbookFormat.values();
    }

    @GetMapping("/upload")
    public String showUploadForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new UploadEbookForm());
        }
        return VIEW_UPLOAD_FORM;
    }

    @PostMapping("/upload")
    public String handleUpload(@Valid @ModelAttribute("form") UploadEbookForm form,
                               BindingResult bindingResult,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return VIEW_UPLOAD_FORM;
        }

        try {
            ebookService.uploadEbook(form);
        } catch (IllegalArgumentException | StorageException ex) {
            bindingResult.reject("upload.error", ex.getMessage());
            return VIEW_UPLOAD_FORM;
        }

        redirectAttributes.addFlashAttribute("success", "Ebook uploaded successfully and is pending validation.");
        return "redirect:/seller/ebooks/upload";
    }
}
