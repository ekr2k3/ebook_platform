Dưới đây là **implement Sprint 3 bằng code Spring Boot** để nối trực tiếp từ Sprint 1 và Sprint 2.

Sprint 3 gồm:

- preview ebook cho buyer
    
- preview ebook cho seller trước publish
    
- protection type
    
- seller dashboard
    
- admin user management
    
- takedown ebook
    
- search phân trang
    
- cache policy public/private cơ bản
    

Mình sẽ giữ đúng phong cách triển khai như 2 sprint trước để bạn ghép vào project dễ nhất.

---

# 1. Mục tiêu Sprint 3

Sau Sprint 3, hệ thống có thể:

- seller chọn kiểu bảo vệ ebook
    
- buyer xem preview trước khi mua
    
- seller xem preview trước khi gửi duyệt
    
- seller xem dashboard doanh thu
    
- admin khóa/mở user
    
- admin takedown ebook
    
- search có phân trang
    
- SSR page public/private có cache policy cơ bản
    

---

# 2. Bổ sung package

```text
src/main/java/com/example/ebook
├── controller
│   ├── PreviewController.java
│   ├── SellerDashboardController.java
│   ├── AdminUserController.java
│   ├── AdminModerationController.java
│   └── AdminSettingsController.java
├── dto
│   ├── EbookUpdateProtectionRequest.java
│   ├── RejectEbookRequest.java
│   └── UserStatusUpdateRequest.java
├── entity
│   └── SystemSetting.java
├── enums
│   ├── ProtectionType.java
│   └── SearchSortType.java
├── repository
│   ├── SystemSettingRepository.java
│   └── DashboardRepository.java
├── service
│   ├── PreviewService.java
│   ├── ProtectionService.java
│   ├── SellerDashboardService.java
│   ├── UserManagementService.java
│   ├── ModerationService.java
│   └── SystemSettingService.java
```

---

# 3. Enum mới

## ProtectionType.java

```java
package com.example.ebook.enums;

public enum ProtectionType {
    NONE,
    WATERMARK,
    RESTRICTED_ACCESS
}
```

---

# 4. Update Entity `Ebook`

Bổ sung các field mới cho Sprint 3.

## Ebook.java

```java
package com.example.ebook.entity;

import com.example.ebook.enums.EbookStatus;
import com.example.ebook.enums.ProtectionType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ebooks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ebook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    private String category;
    private String language;

    @Column(name = "file_url", nullable = false)
    private String fileUrl;

    @Column(name = "cover_url")
    private String coverUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EbookStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "protection_type")
    private ProtectionType protectionType;

    @Column(name = "sample_file_url")
    private String sampleFileUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_by", nullable = false)
    private User seller;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

---

# 5. Update `EbookStatus`

## EbookStatus.java

```java
package com.example.ebook.enums;

public enum EbookStatus {
    PENDING,
    APPROVED,
    REJECTED,
    TAKEDOWN
}
```

---

# 6. Entity mới: SystemSetting

## SystemSetting.java

```java
package com.example.ebook.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "system_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", unique = true, nullable = false)
    private String settingKey;

    @Column(name = "setting_value", nullable = false)
    private String settingValue;
}
```

---

# 7. DTO mới

## EbookUpdateProtectionRequest.java

```java
package com.example.ebook.dto;

import com.example.ebook.enums.ProtectionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EbookUpdateProtectionRequest {
    @NotNull
    private ProtectionType protectionType;
}
```

## RejectEbookRequest.java

```java
package com.example.ebook.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectEbookRequest {
    @NotBlank
    private String reason;
}
```

## UserStatusUpdateRequest.java

```java
package com.example.ebook.dto;

import com.example.ebook.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusUpdateRequest {
    @NotNull
    private UserStatus status;
}
```

---

# 8. Repository update

## EbookRepository.java

Bổ sung query phân trang, dashboard và seller preview.

```java
package com.example.ebook.repository;

import com.example.ebook.entity.Ebook;
import com.example.ebook.entity.User;
import com.example.ebook.enums.EbookStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EbookRepository extends JpaRepository<Ebook, Long> {

    List<Ebook> findByStatus(EbookStatus status);

    Optional<Ebook> findByIdAndStatus(Long id, EbookStatus status);

    Optional<Ebook> findByIdAndSeller(Long id, User seller);

    List<Ebook> findBySeller(User seller);

    @Query("""
        SELECT e FROM Ebook e
        WHERE e.status = :status
        AND (
            LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(e.author) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
    """)
    Page<Ebook> searchApproved(@Param("status") EbookStatus status,
                               @Param("keyword") String keyword,
                               Pageable pageable);

    long countBySeller(User seller);

    long countBySellerAndStatus(User seller, EbookStatus status);
}
```

## SystemSettingRepository.java

```java
package com.example.ebook.repository;

import com.example.ebook.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {
    Optional<SystemSetting> findBySettingKey(String settingKey);
}
```

## UserRepository.java

Bổ sung search/filter cho admin.

```java
package com.example.ebook.repository;

import com.example.ebook.entity.User;
import com.example.ebook.enums.Role;
import com.example.ebook.enums.UserStatus;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Page<User> findByEmailContainingIgnoreCaseAndStatus(String email, UserStatus status, Pageable pageable);
    Page<User> findByRole(Role role, Pageable pageable);
}
```

---

# 9. Service mới

## SystemSettingService.java

```java
package com.example.ebook.service;

import com.example.ebook.entity.SystemSetting;
import com.example.ebook.enums.ProtectionType;
import com.example.ebook.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    public String getValue(String key, String defaultValue) {
        return systemSettingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .orElse(defaultValue);
    }

    public void saveValue(String key, String value) {
        SystemSetting setting = systemSettingRepository.findBySettingKey(key)
                .orElse(SystemSetting.builder().settingKey(key).build());

        setting.setSettingValue(value);
        systemSettingRepository.save(setting);
    }

    public ProtectionType getDefaultProtectionType() {
        String value = getValue("default_protection_type", "NONE");
        return ProtectionType.valueOf(value);
    }

    public boolean isPreviewEnabled() {
        return Boolean.parseBoolean(getValue("preview_enabled", "true"));
    }

    public String getWatermarkPattern() {
        return getValue("watermark_pattern", "Buyer: {email}");
    }
}
```

## ProtectionService.java

```java
package com.example.ebook.service;

import com.example.ebook.entity.Ebook;
import com.example.ebook.entity.User;
import com.example.ebook.enums.ProtectionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

@Service
@RequiredArgsConstructor
public class ProtectionService {

    private final SystemSettingService systemSettingService;

    public String resolveWatermarkText(User user, Ebook ebook) {
        String pattern = systemSettingService.getWatermarkPattern();
        return pattern
                .replace("{email}", user.getEmail())
                .replace("{ebookTitle}", ebook.getTitle())
                .replace("{buyerName}", user.getFullName());
    }

    public void applyReaderModel(User user, Ebook ebook, Model model) {
        ProtectionType protectionType = ebook.getProtectionType() == null
                ? ProtectionType.NONE
                : ebook.getProtectionType();

        model.addAttribute("protectionType", protectionType);

        if (protectionType == ProtectionType.WATERMARK) {
            model.addAttribute("watermarkText", resolveWatermarkText(user, ebook));
        }
    }
}
```

## PreviewService.java

```java
package com.example.ebook.service;

import com.example.ebook.entity.Ebook;
import com.example.ebook.entity.User;
import com.example.ebook.enums.EbookStatus;
import com.example.ebook.repository.EbookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PreviewService {

    private final EbookRepository ebookRepository;
    private final SystemSettingService systemSettingService;

    public Ebook getPublicPreview(Long ebookId) {
        if (!systemSettingService.isPreviewEnabled()) {
            throw new RuntimeException("Hệ thống đang tắt preview");
        }

        Ebook ebook = ebookRepository.findByIdAndStatus(ebookId, EbookStatus.APPROVED)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ebook"));

        if (ebook.getSampleFileUrl() == null || ebook.getSampleFileUrl().isBlank()) {
            throw new RuntimeException("Ebook chưa có preview");
        }

        return ebook;
    }

    public Ebook getSellerPreview(Long ebookId, User seller) {
        return ebookRepository.findByIdAndSeller(ebookId, seller)
                .orElseThrow(() -> new RuntimeException("Bạn không có quyền preview ebook này"));
    }
}
```

## UserManagementService.java

```java
package com.example.ebook.service;

import com.example.ebook.entity.User;
import com.example.ebook.enums.UserStatus;
import com.example.ebook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private final UserRepository userRepository;

    public Page<User> getUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return userRepository.findAll(pageable);
    }

    public void updateStatus(Long userId, UserStatus status, Long currentAdminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        if (user.getId().equals(currentAdminId) && status == UserStatus.INACTIVE) {
            throw new RuntimeException("Admin không thể tự khóa chính mình");
        }

        user.setStatus(status);
        userRepository.save(user);
    }
}
```

## ModerationService.java

```java
package com.example.ebook.service;

import com.example.ebook.entity.Ebook;
import com.example.ebook.enums.EbookStatus;
import com.example.ebook.repository.EbookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ModerationService {

    private final EbookRepository ebookRepository;

    public void takedown(Long ebookId, String reason) {
        Ebook ebook = ebookRepository.findById(ebookId)
                .orElseThrow(() -> new RuntimeException("Ebook không tồn tại"));

        ebook.setStatus(EbookStatus.TAKEDOWN);
        ebook.setRejectionReason(reason);
        ebook.setUpdatedAt(LocalDateTime.now());

        ebookRepository.save(ebook);
    }
}
```

## SellerDashboardService.java

```java
package com.example.ebook.service;

import com.example.ebook.entity.User;
import com.example.ebook.enums.EbookStatus;
import com.example.ebook.enums.OrderStatus;
import com.example.ebook.repository.EbookRepository;
import com.example.ebook.repository.OrderRepository;
import lombok.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SellerDashboardService {

    private final EbookRepository ebookRepository;
    private final OrderRepository orderRepository;

    public SellerDashboardData getDashboard(User seller) {
        long totalEbooks = ebookRepository.countBySeller(seller);
        long approvedEbooks = ebookRepository.countBySellerAndStatus(seller, EbookStatus.APPROVED);

        BigDecimal revenue = orderRepository.sumRevenueBySellerAndStatus(seller.getId(), OrderStatus.SUCCESS);
        Long soldCount = orderRepository.countSoldItemsBySellerAndStatus(seller.getId(), OrderStatus.SUCCESS);

        return SellerDashboardData.builder()
                .totalEbooks(totalEbooks)
                .approvedEbooks(approvedEbooks)
                .soldCount(soldCount == null ? 0L : soldCount)
                .revenue(revenue == null ? BigDecimal.ZERO : revenue)
                .build();
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SellerDashboardData {
        private long totalEbooks;
        private long approvedEbooks;
        private long soldCount;
        private BigDecimal revenue;
    }
}
```

---

# 10. Update OrderRepository cho dashboard

## OrderRepository.java

```java
package com.example.ebook.repository;

import com.example.ebook.entity.Order;
import com.example.ebook.entity.User;
import com.example.ebook.enums.OrderStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBuyer(User buyer);

    List<Order> findByStatus(OrderStatus status);

    Optional<Order> findByPaymentRef(String paymentRef);

    @Query("""
        SELECT COALESCE(SUM(oi.price), 0)
        FROM Order o
        JOIN o.items oi
        JOIN oi.ebook e
        WHERE e.seller.id = :sellerId
        AND o.status = :status
    """)
    BigDecimal sumRevenueBySellerAndStatus(@Param("sellerId") Long sellerId,
                                           @Param("status") OrderStatus status);

    @Query("""
        SELECT COUNT(oi.id)
        FROM Order o
        JOIN o.items oi
        JOIN oi.ebook e
        WHERE e.seller.id = :sellerId
        AND o.status = :status
    """)
    Long countSoldItemsBySellerAndStatus(@Param("sellerId") Long sellerId,
                                         @Param("status") OrderStatus status);
}
```

---

# 11. Update EbookService cho protection + sample + search page

## EbookService.java

```java
package com.example.ebook.service;

import com.example.ebook.dto.EbookCreateRequest;
import com.example.ebook.entity.Ebook;
import com.example.ebook.entity.User;
import com.example.ebook.enums.EbookStatus;
import com.example.ebook.repository.EbookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EbookService {

    private final EbookRepository ebookRepository;
    private final FileStorageService fileStorageService;
    private final SystemSettingService systemSettingService;

    public void createEbook(EbookCreateRequest request, User seller) {
        if (!fileStorageService.isValidEbookFile(request.getFile())) {
            throw new RuntimeException("Chỉ chấp nhận file PDF hoặc EPUB");
        }

        String fileUrl = fileStorageService.store(request.getFile());

        // Sprint 3: tạm dùng luôn file chính làm sample demo
        String sampleFileUrl = fileUrl;

        Ebook ebook = Ebook.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .language(request.getLanguage())
                .fileUrl(fileUrl)
                .sampleFileUrl(sampleFileUrl)
                .status(EbookStatus.PENDING)
                .protectionType(systemSettingService.getDefaultProtectionType())
                .seller(seller)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ebookRepository.save(ebook);
    }

    public Page<Ebook> searchApprovedPage(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());

        String safeKeyword = keyword == null ? "" : keyword.trim();
        return ebookRepository.searchApproved(EbookStatus.APPROVED, safeKeyword, pageable);
    }

    public Ebook getApprovedDetail(Long id) {
        return ebookRepository.findByIdAndStatus(id, EbookStatus.APPROVED)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ebook"));
    }

    public void updateProtection(Long ebookId, User seller, com.example.ebook.enums.ProtectionType protectionType) {
        Ebook ebook = ebookRepository.findByIdAndSeller(ebookId, seller)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ebook"));

        ebook.setProtectionType(protectionType);
        ebook.setUpdatedAt(LocalDateTime.now());
        ebookRepository.save(ebook);
    }
}
```

---

# 12. Update ReaderService dùng ProtectionService

## ReaderService.java

```java
package com.example.ebook.service;

import com.example.ebook.entity.Ebook;
import com.example.ebook.entity.User;
import com.example.ebook.repository.EbookRepository;
import com.example.ebook.repository.UserEbookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

@Service
@RequiredArgsConstructor
public class ReaderService {

    private final EbookRepository ebookRepository;
    private final UserEbookRepository userEbookRepository;
    private final ProtectionService protectionService;

    public Ebook openBook(Long ebookId, User buyer, Model model) {
        Ebook ebook = ebookRepository.findById(ebookId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ebook"));

        boolean hasAccess = userEbookRepository.existsByBuyerAndEbook(buyer, ebook);
        if (!hasAccess) {
            throw new RuntimeException("Bạn không có quyền truy cập ebook này");
        }

        protectionService.applyReaderModel(buyer, ebook, model);
        return ebook;
    }
}
```

---

# 13. Controller mới

## PreviewController.java

```java
package com.example.ebook.controller;

import com.example.ebook.entity.Ebook;
import com.example.ebook.entity.User;
import com.example.ebook.security.CustomUserDetails;
import com.example.ebook.service.PreviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class PreviewController {

    private final PreviewService previewService;

    @GetMapping("/ebooks/{id}/preview")
    public String publicPreview(@PathVariable Long id, Model model) {
        Ebook ebook = previewService.getPublicPreview(id);
        model.addAttribute("ebook", ebook);
        return "public/preview";
    }

    @GetMapping("/seller/ebooks/{id}/preview")
    public String sellerPreview(@PathVariable Long id,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                Model model) {
        User seller = userDetails.getUser();
        Ebook ebook = previewService.getSellerPreview(id, seller);
        model.addAttribute("ebook", ebook);
        return "seller/preview";
    }
}
```

## SellerDashboardController.java

```java
package com.example.ebook.controller;

import com.example.ebook.entity.User;
import com.example.ebook.security.CustomUserDetails;
import com.example.ebook.service.SellerDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/seller")
public class SellerDashboardController {

    private final SellerDashboardService sellerDashboardService;

    @GetMapping("/dashboard-v2")
    public String dashboard(@AuthenticationPrincipal CustomUserDetails userDetails,
                            Model model) {
        User seller = userDetails.getUser();
        model.addAttribute("dashboard", sellerDashboardService.getDashboard(seller));
        return "seller/dashboard-v2";
    }
}
```

## AdminUserController.java

```java
package com.example.ebook.controller;

import com.example.ebook.entity.User;
import com.example.ebook.enums.UserStatus;
import com.example.ebook.security.CustomUserDetails;
import com.example.ebook.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserManagementService userManagementService;

    @GetMapping
    public String listUsers(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "10") int size,
                            Model model) {
        Page<User> users = userManagementService.getUsers(page, size);
        model.addAttribute("users", users);
        return "admin/users";
    }

    @PostMapping("/{id}/block")
    public String blockUser(@PathVariable Long id,
                            @AuthenticationPrincipal CustomUserDetails currentAdmin) {
        userManagementService.updateStatus(id, UserStatus.INACTIVE, currentAdmin.getUser().getId());
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/unblock")
    public String unblockUser(@PathVariable Long id,
                              @AuthenticationPrincipal CustomUserDetails currentAdmin) {
        userManagementService.updateStatus(id, UserStatus.ACTIVE, currentAdmin.getUser().getId());
        return "redirect:/admin/users";
    }
}
```

## AdminModerationController.java

```java
package com.example.ebook.controller;

import com.example.ebook.service.ModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/ebooks")
public class AdminModerationController {

    private final ModerationService moderationService;

    @PostMapping("/{id}/takedown")
    public String takedown(@PathVariable Long id,
                           @RequestParam String reason) {
        moderationService.takedown(id, reason);
        return "redirect:/admin/ebooks/pending";
    }
}
```

## AdminSettingsController.java

```java
package com.example.ebook.controller;

import com.example.ebook.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/settings")
public class AdminSettingsController {

    private final SystemSettingService systemSettingService;

    @GetMapping
    public String settingsPage(Model model) {
        model.addAttribute("defaultProtectionType",
                systemSettingService.getValue("default_protection_type", "NONE"));
        model.addAttribute("previewEnabled",
                systemSettingService.getValue("preview_enabled", "true"));
        model.addAttribute("watermarkPattern",
                systemSettingService.getValue("watermark_pattern", "Buyer: {email}"));
        return "admin/settings";
    }

    @PostMapping
    public String saveSettings(@RequestParam String defaultProtectionType,
                               @RequestParam String previewEnabled,
                               @RequestParam String watermarkPattern) {

        systemSettingService.saveValue("default_protection_type", defaultProtectionType);
        systemSettingService.saveValue("preview_enabled", previewEnabled);
        systemSettingService.saveValue("watermark_pattern", watermarkPattern);

        return "redirect:/admin/settings?saved";
    }
}
```

---

# 14. Update PublicController với search phân trang

## PublicController.java

```java
package com.example.ebook.controller;

import com.example.ebook.service.EbookService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.ebook.entity.Ebook;

@Controller
@RequiredArgsConstructor
public class PublicController {

    private final EbookService ebookService;

    @GetMapping("/")
    public String home(Model model) {
        Page<Ebook> page = ebookService.searchApprovedPage("", 0, 8);
        model.addAttribute("pageData", page);
        model.addAttribute("ebooks", page.getContent());
        return "public/home";
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false, defaultValue = "") String keyword,
                         @RequestParam(defaultValue = "0") int page,
                         @RequestParam(defaultValue = "8") int size,
                         Model model) {
        Page<Ebook> pageData = ebookService.searchApprovedPage(keyword, page, size);
        model.addAttribute("pageData", pageData);
        model.addAttribute("ebooks", pageData.getContent());
        model.addAttribute("keyword", keyword);
        return "public/search";
    }

    @GetMapping("/ebooks/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("ebook", ebookService.getApprovedDetail(id));
        return "public/detail";
    }
}
```

---

# 15. Update ReaderController

## ReaderController.java

```java
package com.example.ebook.controller;

import com.example.ebook.entity.Ebook;
import com.example.ebook.entity.User;
import com.example.ebook.security.CustomUserDetails;
import com.example.ebook.service.ReaderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/reader")
public class ReaderController {

    private final ReaderService readerService;

    @GetMapping("/{ebookId}")
    public String openBook(@PathVariable Long ebookId,
                           @AuthenticationPrincipal CustomUserDetails userDetails,
                           Model model) {
        User buyer = userDetails.getUser();
        Ebook ebook = readerService.openBook(ebookId, buyer, model);
        model.addAttribute("ebook", ebook);
        return "buyer/reader";
    }
}
```

---

# 16. Cache policy filter/interceptor cơ bản

Bạn có thể xử lý cache public/private ở layer web.

## CacheControlInterceptor.java

```java
package com.example.ebook.config;

import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class CacheControlInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        String uri = request.getRequestURI();

        if (uri.startsWith("/library")
                || uri.startsWith("/reader")
                || uri.startsWith("/seller")
                || uri.startsWith("/admin")) {
            response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        } else {
            response.setHeader("Cache-Control", "public, max-age=60");
        }

        return true;
    }
}
```

## WebMvcConfig.java

```java
package com.example.ebook.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final CacheControlInterceptor cacheControlInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(cacheControlInterceptor);
    }
}
```

---

# 17. Update Security cho user bị khóa

Cần chặn user `INACTIVE`.

## CustomUserDetails.java

```java
@Override
public boolean isEnabled() {
    return user.getStatus() == com.example.ebook.enums.UserStatus.ACTIVE;
}
```

---

# 18. Update form seller để chọn protection

## `templates/seller/create-ebook.html`

Thêm select protection nếu muốn seller tự chọn ngay từ lúc tạo. Nếu không, có thể làm màn chỉnh sửa sau.

```html
<select name="protectionType">
    <option value="NONE">NONE</option>
    <option value="WATERMARK">WATERMARK</option>
    <option value="RESTRICTED_ACCESS">RESTRICTED_ACCESS</option>
</select>
```

Nếu dùng cách này, bạn cần update DTO `EbookCreateRequest`.

## EbookCreateRequest.java

```java
package com.example.ebook.dto;

import com.example.ebook.enums.ProtectionType;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;

@Data
public class EbookCreateRequest {

    @NotBlank
    private String title;

    @NotBlank
    private String author;

    @NotBlank
    private String description;

    @NotNull
    @Positive
    private BigDecimal price;

    @NotBlank
    private String category;

    @NotBlank
    private String language;

    @NotNull
    private MultipartFile file;

    private ProtectionType protectionType;
}
```

Và update service:

```java
.protectionType(request.getProtectionType() != null
        ? request.getProtectionType()
        : systemSettingService.getDefaultProtectionType())
```

---

# 19. Thymeleaf page mới

## `templates/public/preview.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Preview ebook</title>
</head>
<body>
<h1>Preview: <span th:text="${ebook.title}"></span></h1>
<p>Tác giả: <span th:text="${ebook.author}"></span></p>
<p>Sample file: <span th:text="${ebook.sampleFileUrl}"></span></p>
<p>Đây là preview giới hạn trước khi mua.</p>
</body>
</html>
```

## `templates/seller/preview.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Seller Preview</title>
</head>
<body>
<h1>Seller Preview: <span th:text="${ebook.title}"></span></h1>
<p>Tác giả: <span th:text="${ebook.author}"></span></p>
<p>File: <span th:text="${ebook.fileUrl}"></span></p>
<p>Đây là bản preview nội bộ cho seller trước khi gửi duyệt.</p>
</body>
</html>
```

## `templates/seller/dashboard-v2.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Seller Dashboard</title>
</head>
<body>
<h1>Seller Dashboard</h1>

<p>Tổng ebook: <span th:text="${dashboard.totalEbooks}"></span></p>
<p>Ebook đã duyệt: <span th:text="${dashboard.approvedEbooks}"></span></p>
<p>Số lượt bán: <span th:text="${dashboard.soldCount}"></span></p>
<p>Doanh thu: <span th:text="${dashboard.revenue}"></span></p>
</body>
</html>
```

## `templates/admin/users.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Admin Users</title>
</head>
<body>
<h1>Danh sách user</h1>

<table border="1">
    <tr>
        <th>ID</th>
        <th>Email</th>
        <th>Role</th>
        <th>Status</th>
        <th>Action</th>
    </tr>

    <tr th:each="user : ${users.content}">
        <td th:text="${user.id}"></td>
        <td th:text="${user.email}"></td>
        <td th:text="${user.role}"></td>
        <td th:text="${user.status}"></td>
        <td>
            <form th:if="${user.status.name() == 'ACTIVE'}"
                  th:action="@{'/admin/users/' + ${user.id} + '/block'}"
                  method="post">
                <button type="submit">Block</button>
            </form>

            <form th:if="${user.status.name() == 'INACTIVE'}"
                  th:action="@{'/admin/users/' + ${user.id} + '/unblock'}"
                  method="post">
                <button type="submit">Unblock</button>
            </form>
        </td>
    </tr>
</table>
</body>
</html>
```

## `templates/admin/settings.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>System Settings</title>
</head>
<body>
<h1>System Settings</h1>

<form th:action="@{/admin/settings}" method="post">
    <div>
        <label>Default Protection Type</label>
        <select name="defaultProtectionType">
            <option value="NONE" th:selected="${defaultProtectionType == 'NONE'}">NONE</option>
            <option value="WATERMARK" th:selected="${defaultProtectionType == 'WATERMARK'}">WATERMARK</option>
            <option value="RESTRICTED_ACCESS" th:selected="${defaultProtectionType == 'RESTRICTED_ACCESS'}">RESTRICTED_ACCESS</option>
        </select>
    </div>

    <div>
        <label>Preview Enabled</label>
        <select name="previewEnabled">
            <option value="true" th:selected="${previewEnabled == 'true'}">true</option>
            <option value="false" th:selected="${previewEnabled == 'false'}">false</option>
        </select>
    </div>

    <div>
        <label>Watermark Pattern</label>
        <input type="text" name="watermarkPattern" th:value="${watermarkPattern}" />
    </div>

    <button type="submit">Save</button>
</form>
</body>
</html>
```

## `templates/buyer/reader.html`

Update để hiển thị watermark nếu có.

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Reader</title>
    <style>
        .watermark {
            position: fixed;
            top: 20px;
            right: 20px;
            opacity: 0.25;
            font-size: 14px;
            transform: rotate(-15deg);
        }
    </style>
</head>
<body>
<div th:if="${protectionType != null and protectionType.name() == 'WATERMARK'}"
     class="watermark"
     th:text="${watermarkText}">
</div>

<h1 th:text="${ebook.title}"></h1>
<p><strong>Tác giả:</strong> <span th:text="${ebook.author}"></span></p>
<p><strong>Protection:</strong> <span th:text="${protectionType}"></span></p>
<p>File: <span th:text="${ebook.fileUrl}"></span></p>
</body>
</html>
```

## `templates/public/detail.html`

Bổ sung link preview.

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Chi tiết ebook</title>
</head>
<body>
<h1 th:text="${ebook.title}"></h1>
<p><strong>Tác giả:</strong> <span th:text="${ebook.author}"></span></p>
<p><strong>Giá:</strong> <span th:text="${ebook.price}"></span></p>
<p><strong>Mô tả:</strong> <span th:text="${ebook.description}"></span></p>
<p><strong>Bảo vệ:</strong> <span th:text="${ebook.protectionType}"></span></p>

<p>
    <a th:href="@{'/ebooks/' + ${ebook.id} + '/preview'}">Xem preview</a>
</p>

<form th:action="@{/orders/create}" method="post">
    <input type="hidden" name="ebookId" th:value="${ebook.id}" />
    <button type="submit">Mua ebook</button>
</form>
</body>
</html>
```

---

# 20. Seed system settings mặc định

Trong `CommandLineRunner`, thêm:

```java
@Bean
CommandLineRunner initSettings(SystemSettingRepository repo) {
    return args -> {
        if (repo.findBySettingKey("default_protection_type").isEmpty()) {
            repo.save(SystemSetting.builder()
                    .settingKey("default_protection_type")
                    .settingValue("NONE")
                    .build());
        }

        if (repo.findBySettingKey("preview_enabled").isEmpty()) {
            repo.save(SystemSetting.builder()
                    .settingKey("preview_enabled")
                    .settingValue("true")
                    .build());
        }

        if (repo.findBySettingKey("watermark_pattern").isEmpty()) {
            repo.save(SystemSetting.builder()
                    .settingKey("watermark_pattern")
                    .settingValue("Buyer: {email}")
                    .build());
        }
    };
}
```

---

# 21. Những gì cần test trong Sprint 3

## Preview

- ebook approved có preview truy cập được
    
- preview bị tắt thì không xem được
    
- seller chỉ preview được sách của mình
    

## Protection

- NONE: reader bình thường
    
- WATERMARK: hiển thị watermark
    
- RESTRICTED_ACCESS: vẫn phải có ownership mới đọc
    

## Seller Dashboard

- total ebook đúng
    
- doanh thu chỉ tính order success
    

## Admin User

- block user được
    
- unblock user được
    
- admin không tự khóa mình
    

## Takedown

- ebook bị takedown không còn ở search/detail approved
    
- không mua mới được nữa
    

## Search Pagination

- page 0/page 1 hoạt động
    
- chỉ hiện approved
    

## Cache

- library/reader/seller/admin có `no-store`
    
- home/search/detail có `public, max-age=60`
    

---

# 22. Các rủi ro kỹ thuật cần chú ý

## 1. Lazy loading trong Thymeleaf

Nếu render `seller`, `buyer`, `order.items` mà session JPA đã đóng, có thể lỗi.

Giải pháp:

- fetch đủ dữ liệu ở service
    
- hoặc dùng DTO projection
    

## 2. Takedown ảnh hưởng buyer cũ

Hiện tại code chỉ chặn bán mới vì catalog chỉ hiện approved.  
Reader vẫn đọc được nếu buyer đã mua.

Đây là quyết định nghiệp vụ. Nếu muốn chặn cả user cũ, phải check thêm ở `ReaderService`.

## 3. Preview đang dùng sample file = file gốc

Đây chỉ là bản demo Sprint 3.  
Sau này nên:

- cắt PDF sample
    
- sinh EPUB sample riêng
    

## 4. Cache đơn giản

Interceptor hiện tại là mức cơ bản.  
Production nên tách theo static assets / CDN / reverse proxy.

---

# 23. Kết quả sau khi code xong Sprint 3

Bạn sẽ có:

- preview trước khi mua
    
- seller preview
    
- protection cơ bản
    
- seller dashboard
    
- admin user management
    
- takedown ebook
    
- search phân trang
    
- cache policy cơ bản cho SSR
    

Tới đây hệ thống đã khá đầy đủ cho một **MVP đồ án rất ổn**.

---

# 24. Bước tiếp theo hợp lý nhất

Sau Sprint 3, nên làm một trong các phần này:

- tích hợp **PDF.js reader thật**
    
- làm **REST API documentation**
    
- refactor sang **DTO + exception handler + response chuẩn**
    
- viết **sequence diagram + class diagram final**
    
- đóng gói **Deployment diagram**
    

Mình khuyên bước tiếp theo nên là **refactor production-ready** vì code hiện tại đủ nghiệp vụ nhưng vẫn còn theo kiểu sprint-demo.

Nếu bạn muốn, mình sẽ làm tiếp bản:  
**"refactor toàn bộ project theo chuẩn production Spring Boot"**.