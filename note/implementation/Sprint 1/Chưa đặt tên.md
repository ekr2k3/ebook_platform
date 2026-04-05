Dưới đây là **chi tiết implement Sprint 1** cho hệ thống ebook dùng **Spring Boot monolith SSR**.

Mình sẽ đi theo hướng:

- Spring Boot
    
- Spring MVC
    
- Thymeleaf
    
- Spring Security
    
- Spring Data JPA
    
- MySQL/PostgreSQL
    

Phạm vi Sprint 1:

- đăng ký
    
- đăng nhập
    
- phân quyền
    
- seller upload ebook
    
- seller nhập metadata
    
- admin duyệt ebook
    
- buyer tìm kiếm ebook
    
- buyer xem chi tiết ebook
    

---

# 1. Cấu trúc project

```text
src/main/java/com/example/ebook
├── config
│   └── SecurityConfig.java
├── controller
│   ├── AuthController.java
│   ├── PublicController.java
│   ├── SellerEbookController.java
│   └── AdminEbookController.java
├── dto
│   ├── RegisterRequest.java
│   ├── EbookCreateRequest.java
│   └── EbookSearchRequest.java
├── entity
│   ├── User.java
│   └── Ebook.java
├── enums
│   ├── Role.java
│   ├── UserStatus.java
│   └── EbookStatus.java
├── repository
│   ├── UserRepository.java
│   └── EbookRepository.java
├── security
│   ├── CustomUserDetails.java
│   ├── CustomUserDetailsService.java
│   └── CustomAuthenticationSuccessHandler.java
├── service
│   ├── AuthService.java
│   ├── EbookService.java
│   ├── AdminEbookService.java
│   └── FileStorageService.java
└── EbookApplication.java
```

---

# 2. Dependency cần có

## Maven `pom.xml`

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

---

# 3. Cấu hình `application.yml`

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ebook_db?useSSL=false&serverTimezone=UTC
    username: root
    password: 123456

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 20MB

file:
  upload-dir: uploads/
```

---

# 4. Enum

## Role.java

```java
package com.example.ebook.enums;

public enum Role {
    BUYER, SELLER, ADMIN
}
```

## UserStatus.java

```java
package com.example.ebook.enums;

public enum UserStatus {
    ACTIVE, INACTIVE
}
```

## EbookStatus.java

```java
package com.example.ebook.enums;

public enum EbookStatus {
    PENDING, APPROVED, REJECTED
}
```

---

# 5. Entity

## User.java

```java
package com.example.ebook.entity;

import com.example.ebook.enums.Role;
import com.example.ebook.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
```

## Ebook.java

```java
package com.example.ebook.entity;

import com.example.ebook.enums.EbookStatus;
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

# 6. DTO

## RegisterRequest.java

```java
package com.example.ebook.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    private String email;

    @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự")
    private String password;
}
```

## EbookCreateRequest.java

```java
package com.example.ebook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
}
```

---

# 7. Repository

## UserRepository.java

```java
package com.example.ebook.repository;

import com.example.ebook.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

## EbookRepository.java

```java
package com.example.ebook.repository;

import com.example.ebook.entity.Ebook;
import com.example.ebook.entity.User;
import com.example.ebook.enums.EbookStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EbookRepository extends JpaRepository<Ebook, Long> {

    List<Ebook> findByStatus(EbookStatus status);

    Optional<Ebook> findByIdAndStatus(Long id, EbookStatus status);

    List<Ebook> findBySeller(User seller);

    List<Ebook> findByStatusAndTitleContainingIgnoreCaseOrStatusAndAuthorContainingIgnoreCase(
            EbookStatus status1, String titleKeyword,
            EbookStatus status2, String authorKeyword
    );
}
```

Gợi ý tốt hơn về sau là dùng `@Query`, nhưng sprint 1 có thể đi nhanh như vậy.

---

# 8. FileStorageService

## FileStorageService.java

```java
package com.example.ebook.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    public String store(MultipartFile file) {
        try {
            String originalName = StringUtils.cleanPath(file.getOriginalFilename());
            String extension = "";

            int dotIndex = originalName.lastIndexOf(".");
            if (dotIndex >= 0) {
                extension = originalName.substring(dotIndex);
            }

            String fileName = UUID.randomUUID() + extension;

            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path targetPath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return targetPath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Không thể lưu file", e);
        }
    }

    public boolean isValidEbookFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;
        String name = file.getOriginalFilename();
        if (name == null) return false;
        String lower = name.toLowerCase();
        return lower.endsWith(".pdf") || lower.endsWith(".epub");
    }
}
```

---

# 9. AuthService

## AuthService.java

```java
package com.example.ebook.service;

import com.example.ebook.dto.RegisterRequest;
import com.example.ebook.entity.User;
import com.example.ebook.enums.Role;
import com.example.ebook.enums.UserStatus;
import com.example.ebook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.BUYER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);
    }
}
```

---

# 10. EbookService

## EbookService.java

```java
package com.example.ebook.service;

import com.example.ebook.dto.EbookCreateRequest;
import com.example.ebook.entity.Ebook;
import com.example.ebook.entity.User;
import com.example.ebook.enums.EbookStatus;
import com.example.ebook.repository.EbookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EbookService {

    private final EbookRepository ebookRepository;
    private final FileStorageService fileStorageService;

    public void createEbook(EbookCreateRequest request, User seller) {
        if (!fileStorageService.isValidEbookFile(request.getFile())) {
            throw new RuntimeException("Chỉ chấp nhận file PDF hoặc EPUB");
        }

        String fileUrl = fileStorageService.store(request.getFile());

        Ebook ebook = Ebook.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .description(request.getDescription())
                .price(request.getPrice())
                .category(request.getCategory())
                .language(request.getLanguage())
                .fileUrl(fileUrl)
                .status(EbookStatus.PENDING)
                .seller(seller)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ebookRepository.save(ebook);
    }

    public List<Ebook> searchApproved(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return ebookRepository.findByStatus(EbookStatus.APPROVED);
        }
        return ebookRepository.findByStatusAndTitleContainingIgnoreCaseOrStatusAndAuthorContainingIgnoreCase(
                EbookStatus.APPROVED, keyword,
                EbookStatus.APPROVED, keyword
        );
    }

    public Ebook getApprovedDetail(Long id) {
        return ebookRepository.findByIdAndStatus(id, EbookStatus.APPROVED)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ebook"));
    }
}
```

---

# 11. AdminEbookService

## AdminEbookService.java

```java
package com.example.ebook.service;

import com.example.ebook.entity.Ebook;
import com.example.ebook.enums.EbookStatus;
import com.example.ebook.repository.EbookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminEbookService {

    private final EbookRepository ebookRepository;

    public List<Ebook> getPendingEbooks() {
        return ebookRepository.findByStatus(EbookStatus.PENDING);
    }

    public void approve(Long id) {
        Ebook ebook = ebookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ebook không tồn tại"));

        ebook.setStatus(EbookStatus.APPROVED);
        ebook.setRejectionReason(null);
        ebook.setUpdatedAt(LocalDateTime.now());
        ebookRepository.save(ebook);
    }

    public void reject(Long id, String reason) {
        Ebook ebook = ebookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ebook không tồn tại"));

        ebook.setStatus(EbookStatus.REJECTED);
        ebook.setRejectionReason(reason);
        ebook.setUpdatedAt(LocalDateTime.now());
        ebookRepository.save(ebook);
    }
}
```

---

# 12. Spring Security

## PasswordEncoder bean + security config

## SecurityConfig.java

```java
package com.example.ebook.config;

import com.example.ebook.security.CustomAuthenticationSuccessHandler;
import com.example.ebook.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final CustomAuthenticationSuccessHandler successHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/register", "/login", "/ebooks/**", "/search", "/css/**", "/js/**").permitAll()
                        .requestMatchers("/seller/**").hasRole("SELLER")
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(successHandler)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }
}
```

---

# 13. CustomUserDetails

## CustomUserDetails.java

```java
package com.example.ebook.security;

import com.example.ebook.entity.User;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    @Override
    public List<SimpleGrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }
}
```

## CustomUserDetailsService.java

```java
package com.example.ebook.security;

import com.example.ebook.entity.User;
import com.example.ebook.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy user"));
        return new CustomUserDetails(user);
    }
}
```

## CustomAuthenticationSuccessHandler.java

```java
package com.example.ebook.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String role = userDetails.getUser().getRole().name();

        if ("ADMIN".equals(role)) {
            response.sendRedirect("/admin/dashboard");
        } else if ("SELLER".equals(role)) {
            response.sendRedirect("/seller/dashboard");
        } else {
            response.sendRedirect("/");
        }
    }
}
```

---

# 14. Controller

## AuthController.java

```java
package com.example.ebook.controller;

import com.example.ebook.dto.RegisterRequest;
import com.example.ebook.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                           BindingResult bindingResult,
                           Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            authService.register(request);
            return "redirect:/login?registered";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }
}
```

## PublicController.java

```java
package com.example.ebook.controller;

import com.example.ebook.service.EbookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class PublicController {

    private final EbookService ebookService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("ebooks", ebookService.searchApproved(""));
        return "public/home";
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("ebooks", ebookService.searchApproved(keyword));
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

## SellerEbookController.java

```java
package com.example.ebook.controller;

import com.example.ebook.dto.EbookCreateRequest;
import com.example.ebook.entity.User;
import com.example.ebook.security.CustomUserDetails;
import com.example.ebook.service.EbookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/seller")
@RequiredArgsConstructor
public class SellerEbookController {

    private final EbookService ebookService;

    @GetMapping("/dashboard")
    public String dashboard() {
        return "seller/dashboard";
    }

    @GetMapping("/ebooks/create")
    public String createPage(Model model) {
        model.addAttribute("ebookCreateRequest", new EbookCreateRequest());
        return "seller/create-ebook";
    }

    @PostMapping("/ebooks")
    public String create(@Valid @ModelAttribute("ebookCreateRequest") EbookCreateRequest request,
                         BindingResult result,
                         @AuthenticationPrincipal CustomUserDetails customUserDetails,
                         Model model) {

        if (result.hasErrors()) {
            return "seller/create-ebook";
        }

        try {
            User seller = customUserDetails.getUser();
            ebookService.createEbook(request, seller);
            return "redirect:/seller/dashboard?success";
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return "seller/create-ebook";
        }
    }
}
```

## AdminEbookController.java

```java
package com.example.ebook.controller;

import com.example.ebook.service.AdminEbookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminEbookController {

    private final AdminEbookService adminEbookService;

    @GetMapping("/dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/ebooks/pending")
    public String pending(Model model) {
        model.addAttribute("ebooks", adminEbookService.getPendingEbooks());
        return "admin/pending-ebooks";
    }

    @PostMapping("/ebooks/{id}/approve")
    public String approve(@PathVariable Long id) {
        adminEbookService.approve(id);
        return "redirect:/admin/ebooks/pending";
    }

    @PostMapping("/ebooks/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam String reason) {
        adminEbookService.reject(id, reason);
        return "redirect:/admin/ebooks/pending";
    }
}
```

---

# 15. Thymeleaf page tối thiểu

## `templates/auth/login.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Đăng nhập</title>
</head>
<body>
<h2>Đăng nhập</h2>

<form th:action="@{/login}" method="post">
    <div>
        <label>Email</label>
        <input type="text" name="username"/>
    </div>
    <div>
        <label>Mật khẩu</label>
        <input type="password" name="password"/>
    </div>
    <button type="submit">Đăng nhập</button>
</form>

<p><a th:href="@{/register}">Đăng ký</a></p>
</body>
</html>
```

## `templates/auth/register.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Đăng ký</title>
</head>
<body>
<h2>Đăng ký</h2>

<p th:if="${error}" th:text="${error}" style="color:red"></p>

<form th:action="@{/register}" th:object="${registerRequest}" method="post">
    <div>
        <label>Họ tên</label>
        <input type="text" th:field="*{fullName}">
    </div>
    <div>
        <label>Email</label>
        <input type="email" th:field="*{email}">
    </div>
    <div>
        <label>Mật khẩu</label>
        <input type="password" th:field="*{password}">
    </div>
    <button type="submit">Đăng ký</button>
</form>
</body>
</html>
```

## `templates/public/home.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Trang chủ</title>
</head>
<body>
<h1>Danh sách ebook</h1>

<form th:action="@{/search}" method="get">
    <input type="text" name="keyword" placeholder="Tìm kiếm sách">
    <button type="submit">Tìm</button>
</form>

<div th:each="ebook : ${ebooks}">
    <h3>
        <a th:href="@{'/ebooks/' + ${ebook.id}}" th:text="${ebook.title}"></a>
    </h3>
    <p th:text="${ebook.author}"></p>
</div>
</body>
</html>
```

## `templates/public/detail.html`

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
</body>
</html>
```

## `templates/seller/create-ebook.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Đăng ebook</title>
</head>
<body>
<h2>Đăng ebook</h2>

<p th:if="${error}" th:text="${error}" style="color:red"></p>

<form th:action="@{/seller/ebooks}" th:object="${ebookCreateRequest}" method="post" enctype="multipart/form-data">
    <input type="text" th:field="*{title}" placeholder="Tên sách"><br>
    <input type="text" th:field="*{author}" placeholder="Tác giả"><br>
    <textarea th:field="*{description}" placeholder="Mô tả"></textarea><br>
    <input type="number" step="0.01" th:field="*{price}" placeholder="Giá"><br>
    <input type="text" th:field="*{category}" placeholder="Thể loại"><br>
    <input type="text" th:field="*{language}" placeholder="Ngôn ngữ"><br>
    <input type="file" th:field="*{file}"><br>
    <button type="submit">Lưu</button>
</form>
</body>
</html>
```

## `templates/admin/pending-ebooks.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Ebook chờ duyệt</title>
</head>
<body>
<h2>Danh sách ebook chờ duyệt</h2>

<div th:each="ebook : ${ebooks}">
    <h3 th:text="${ebook.title}"></h3>
    <p th:text="${ebook.author}"></p>

    <form th:action="@{'/admin/ebooks/' + ${ebook.id} + '/approve'}" method="post" style="display:inline;">
        <button type="submit">Approve</button>
    </form>

    <form th:action="@{'/admin/ebooks/' + ${ebook.id} + '/reject'}" method="post" style="display:inline;">
        <input type="text" name="reason" placeholder="Lý do từ chối">
        <button type="submit">Reject</button>
    </form>
</div>
</body>
</html>
```

---

# 16. Seed dữ liệu role SELLER và ADMIN

Vì đăng ký mặc định là BUYER, bạn cần seed sẵn admin/seller.

## `CommandLineRunner` gợi ý

```java
@Bean
CommandLineRunner initUsers(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    return args -> {
        if (userRepository.findByEmail("admin@gmail.com").isEmpty()) {
            userRepository.save(User.builder()
                    .fullName("Admin")
                    .email("admin@gmail.com")
                    .password(passwordEncoder.encode("12345678"))
                    .role(Role.ADMIN)
                    .status(UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        if (userRepository.findByEmail("seller@gmail.com").isEmpty()) {
            userRepository.save(User.builder()
                    .fullName("Seller")
                    .email("seller@gmail.com")
                    .password(passwordEncoder.encode("12345678"))
                    .role(Role.SELLER)
                    .status(UserStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .build());
        }
    };
}
```

---

# 17. Flow chạy Sprint 1

## Buyer

- vào `/`
    
- tìm kiếm sách
    
- xem chi tiết sách approved
    

## Seller

- login bằng seller
    
- vào `/seller/ebooks/create`
    
- upload ebook
    
- ebook ở trạng thái `PENDING`
    

## Admin

- login bằng admin
    
- vào `/admin/ebooks/pending`
    
- approve / reject ebook
    

---

# 18. Những điểm cần test

## Auth

- đăng ký email mới thành công
    
- email trùng bị chặn
    
- đăng nhập đúng/sai
    
- redirect đúng theo role
    

## Seller

- upload pdf/epub thành công
    
- upload file sai định dạng bị chặn
    
- metadata thiếu bị báo lỗi
    

## Admin

- chỉ admin mới vào trang duyệt
    
- approve đổi status đúng
    
- reject lưu đúng reason
    

## Public

- chỉ sách `APPROVED` mới xuất hiện
    
- detail sách chưa duyệt không truy cập được
    

---

# 19. Bug/rủi ro hay gặp

## Query search sai logic OR

Đoạn method name repository có thể khó kiểm soát precedence. An toàn hơn là dùng `@Query`.

Ví dụ tốt hơn:

```java
@Query("""
    SELECT e FROM Ebook e
    WHERE e.status = :status
    AND (
        LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(e.author) LIKE LOWER(CONCAT('%', :keyword, '%'))
    )
""")
List<Ebook> searchApproved(@Param("status") EbookStatus status, @Param("keyword") String keyword);
```

## Upload path

Nếu lưu local path tuyệt đối, deploy môi trường khác dễ lỗi. Sprint 1 chấp nhận được, nhưng nên chuẩn hóa sớm.

## CSRF

Spring Security mặc định bật CSRF. Form POST Thymeleaf cần thêm token nếu có dùng layout thực tế.

Ví dụ:

```html
<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}" />
```

---

# 20. Kết quả sau khi implement xong Sprint 1

Bạn sẽ có:

- hệ thống đăng ký / đăng nhập hoàn chỉnh
    
- phân quyền role
    
- seller đăng ebook được
    
- admin duyệt ebook được
    
- buyer xem catalog công khai được
    

Đây là mốc rất tốt để đi tiếp Sprint 2:

- order
    
- payment
    
- library
    
- reader
    

Nếu bạn muốn, mình có thể làm tiếp phần **implement Sprint 2 chi tiết theo Spring Boot** theo đúng format này.