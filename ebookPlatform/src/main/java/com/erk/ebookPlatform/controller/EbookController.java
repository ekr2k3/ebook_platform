package com.erk.ebookPlatform.controller;

import com.erk.ebookPlatform.dto.UploadEbookForm;
import com.erk.ebookPlatform.enums.EbookFormat;
import com.erk.ebookPlatform.service.EbookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/**
 * Handles seller-facing pages related to ebook upload for Sprint 1 (C1-02).
 */
@Controller
@RequestMapping("/seller/ebooks")
@RequiredArgsConstructor
public class EbookController {

    private static final String VIEW_UPLOAD_FORM = "ebook/upload";

    @Autowired
    private EbookService ebookService;

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
    public String handleUpload(
            @Valid @ModelAttribute("form") UploadEbookForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (form.getFile() == null || form.getFile().isEmpty()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Please select a file to upload.");
            return "redirect:/seller/ebooks/upload";
        }

        if (bindingResult.hasErrors()) {
            System.out.println("===== VALIDATION ERRORS =====");
            bindingResult.getFieldErrors().forEach(error -> {
                System.out.println("Field: " + error.getField());
                System.out.println("Message: " + error.getDefaultMessage());
            });

            bindingResult.getGlobalErrors().forEach(error -> {
                System.out.println("Global: " + error.getDefaultMessage());
            });

            return VIEW_UPLOAD_FORM;
        }

        try {
            ebookService.uploadEbook(form);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Failed to upload ebook: " + ex.getMessage());
            System.err.println("Error in EbookController: " + ex.getMessage());
            return "redirect:/seller/ebooks/upload";
        }

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Ebook uploaded successfully and is pending validation.");
        return "redirect:/seller/ebooks/upload";
    }
}
