Dưới đây là **System Design diagram cho Sprint 1** theo hướng:

- **Spring Boot Monolith**
    
- **SSR**
    
- **Spring Security**
    
- **Spring Data JPA**
    
- **MySQL/PostgreSQL**
    
- flow đúng phạm vi Sprint 1:
    
    - đăng ký / đăng nhập
        
    - phân quyền
        
    - seller upload ebook
        
    - admin duyệt ebook
        
    - buyer search / xem chi tiết
        

---

# 1. Kiến trúc tổng thể Sprint 1

```mermaid
flowchart TD
    A[Browser] --> B[Spring Boot Monolith]

    subgraph B [Spring Boot SSR Monolith]
        C[Controller Layer]
        D[Service Layer]
        E[Security Layer<br/>Spring Security]
        F[Repository Layer<br/>Spring Data JPA]
        G[Thymeleaf SSR Views]
        H[File Storage Service]
    end

    B --> I[(MySQL / PostgreSQL)]
    H --> J[(Local Storage / File Server)]

    C --> D
    D --> F
    F --> I
    D --> H
    H --> J
    E --> C
    C --> G
    G --> A
```

---

# 2. Giải thích các thành phần

## Browser

Người dùng truy cập bằng trình duyệt:

- Buyer
    
- Seller
    
- Admin
    

## Spring Boot Monolith

Một ứng dụng duy nhất xử lý toàn bộ:

- auth
    
- quản lý ebook
    
- moderation
    
- catalog public
    

## Controller Layer

Nhận request HTTP và trả về:

- HTML SSR qua Thymeleaf
    
- redirect sau submit form
    

## Service Layer

Chứa nghiệp vụ:

- đăng ký user
    
- login
    
- upload ebook
    
- duyệt ebook
    
- search ebook
    

## Security Layer

Dùng **Spring Security** để:

- xác thực
    
- phân quyền theo role
    
- bảo vệ route
    

## Repository Layer

Dùng **Spring Data JPA** để thao tác DB.

## Thymeleaf Views

Render server-side:

- login page
    
- register page
    
- seller upload page
    
- admin moderation page
    
- search page
    
- ebook detail page
    

## File Storage Service

Lưu file ebook đã upload:

- local folder
    
- hoặc object storage sau này
    

## Database

Lưu:

- user
    
- ebook metadata
    
- trạng thái duyệt
    

---

# 3. Sơ đồ module nội bộ Spring Boot

```mermaid
flowchart LR
    subgraph Spring Boot Monolith
        A1[Auth Module]
        A2[User Module]
        A3[Ebook Module]
        A4[Admin Moderation Module]
        A5[Public Catalog Module]
        A6[File Storage Module]
    end

    A1 --> A2
    A3 --> A6
    A4 --> A3
    A5 --> A3
```

---

# 4. Các package nên có

```mermaid
flowchart TD
    A[com.example.ebookplatform]
    A --> B[config]
    A --> C[controller]
    A --> D[service]
    A --> E[repository]
    A --> F[entity]
    A --> G[dto]
    A --> H[security]
    A --> I[exception]

    C --> C1[AuthController]
    C --> C2[SellerEbookController]
    C --> C3[AdminEbookController]
    C --> C4[PublicEbookController]

    D --> D1[AuthService]
    D --> D2[EbookService]
    D --> D3[AdminEbookService]
    D --> D4[FileStorageService]

    E --> E1[UserRepository]
    E --> E2[EbookRepository]

    F --> F1[User]
    F --> F2[Ebook]
```

---

# 5. Database design Sprint 1

```mermaid
erDiagram
    USERS ||--o{ EBOOKS : uploads

    USERS {
        bigint id PK
        varchar full_name
        varchar email
        varchar password
        varchar role
        varchar status
        timestamp created_at
    }

    EBOOKS {
        bigint id PK
        varchar title
        varchar author
        text description
        decimal price
        varchar category
        varchar language
        varchar file_url
        varchar cover_url
        varchar status
        text rejection_reason
        bigint upload_by FK
        timestamp created_at
        timestamp updated_at
    }
```

---

# 6. Role-based access design

```mermaid
flowchart TD
    A[User Request] --> B{Authenticated?}

    B -- No --> C[Public Pages Only]
    B -- Yes --> D{Role?}

    D -- BUYER --> E[Search / Detail / Public Access]
    D -- SELLER --> F[Seller Dashboard / Upload Ebook]
    D -- ADMIN --> G[Admin Dashboard / Moderate Ebook]

    F --> H[Cannot access Admin]
    E --> I[Cannot access Seller/Admin]
    G --> J[Full moderation access]
```

---

# 7. SSR flow tổng quát

```mermaid
sequenceDiagram
    participant U as User Browser
    participant C as Spring Controller
    participant S as Service
    participant R as Repository
    participant DB as Database
    participant V as Thymeleaf View

    U->>C: GET /ebooks
    C->>S: getApprovedEbooks()
    S->>R: findApprovedEbooks()
    R->>DB: SELECT * FROM ebooks WHERE status='APPROVED'
    DB-->>R: ebook list
    R-->>S: ebook list
    S-->>C: ebook list
    C->>V: render catalog page
    V-->>U: HTML response
```

---

# 8. Sequence diagram – Đăng ký / đăng nhập

## 8.1 Đăng ký

```mermaid
sequenceDiagram
    participant U as User
    participant AC as AuthController
    participant AS as AuthService
    participant UR as UserRepository
    participant DB as Database

    U->>AC: POST /register
    AC->>AS: register(request)
    AS->>UR: findByEmail(email)
    UR->>DB: SELECT user by email
    DB-->>UR: result
    UR-->>AS: user/null
    AS->>AS: hash password
    AS->>UR: save(user)
    UR->>DB: INSERT user
    DB-->>UR: success
    UR-->>AS: user saved
    AS-->>AC: success
    AC-->>U: redirect /login
```

## 8.2 Đăng nhập

```mermaid
sequenceDiagram
    participant U as User
    participant SC as Spring Security
    participant UDS as UserDetailsService
    participant UR as UserRepository
    participant DB as Database

    U->>SC: POST /login
    SC->>UDS: loadUserByUsername(email)
    UDS->>UR: findByEmail(email)
    UR->>DB: SELECT user by email
    DB-->>UR: user record
    UR-->>UDS: user
    UDS-->>SC: UserDetails
    SC->>SC: verify password
    SC->>SC: create session
    SC-->>U: redirect by role
```

---

# 9. Sequence diagram – Seller upload ebook

```mermaid
sequenceDiagram
    participant S as Seller
    participant EC as SellerEbookController
    participant ES as EbookService
    participant FS as FileStorageService
    participant ER as EbookRepository
    participant DB as Database

    S->>EC: POST /seller/ebooks
    EC->>ES: createEbook(request, file, seller)
    ES->>FS: store(file)
    FS-->>ES: fileUrl
    ES->>ER: save(ebook status=PENDING)
    ER->>DB: INSERT ebook
    DB-->>ER: success
    ER-->>ES: ebook saved
    ES-->>EC: success
    EC-->>S: redirect seller dashboard
```

---

# 10. Sequence diagram – Admin duyệt ebook

```mermaid
sequenceDiagram
    participant A as Admin
    participant AC as AdminEbookController
    participant AS as AdminEbookService
    participant ER as EbookRepository
    participant DB as Database

    A->>AC: POST /admin/ebooks/{id}/approve
    AC->>AS: approveEbook(id)
    AS->>ER: findById(id)
    ER->>DB: SELECT ebook by id
    DB-->>ER: ebook
    ER-->>AS: ebook
    AS->>ER: save(status=APPROVED)
    ER->>DB: UPDATE ebooks
    DB-->>ER: success
    ER-->>AS: updated
    AS-->>AC: success
    AC-->>A: redirect /admin/ebooks/pending
```

---

# 11. Sequence diagram – Buyer search và xem chi tiết

## 11.1 Search

```mermaid
sequenceDiagram
    participant B as Buyer
    participant PC as PublicEbookController
    participant ES as EbookService
    participant ER as EbookRepository
    participant DB as Database

    B->>PC: GET /search?keyword=java
    PC->>ES: searchApprovedEbooks(keyword)
    ES->>ER: searchByKeywordAndStatus(keyword, APPROVED)
    ER->>DB: SELECT ebooks WHERE status='APPROVED'
    DB-->>ER: results
    ER-->>ES: ebook list
    ES-->>PC: ebook list
    PC-->>B: SSR HTML search results
```

## 11.2 Detail

```mermaid
sequenceDiagram
    participant B as Buyer
    participant PC as PublicEbookController
    participant ES as EbookService
    participant ER as EbookRepository
    participant DB as Database

    B->>PC: GET /ebooks/{id}
    PC->>ES: getApprovedEbookDetail(id)
    ES->>ER: findByIdAndStatus(id, APPROVED)
    ER->>DB: SELECT ebook by id and status
    DB-->>ER: ebook
    ER-->>ES: ebook
    ES-->>PC: ebook detail
    PC-->>B: SSR HTML detail page
```

---

# 12. Luồng request thực tế trong Sprint 1

```mermaid
flowchart TD
    A[Buyer/Seller/Admin mở trình duyệt] --> B[Request tới Spring Boot]
    B --> C{Spring Security}

    C -- chưa đăng nhập --> D[Cho vào public route hoặc redirect login]
    C -- đã đăng nhập --> E[Kiểm tra role]

    E --> F[Controller]
    F --> G[Service]
    G --> H[Repository]
    H --> I[(Database)]

    G --> J[File Storage Service]
    J --> K[(File Storage)]

    F --> L[Thymeleaf]
    L --> M[HTML trả về browser]
```

---

# 13. Route map Sprint 1

## Public

- `GET /`
    
- `GET /register`
    
- `POST /register`
    
- `GET /login`
    
- `POST /login`
    
- `GET /ebooks`
    
- `GET /ebooks/{id}`
    
- `GET /search`
    

## Seller

- `GET /seller/dashboard`
    
- `GET /seller/ebooks/create`
    
- `POST /seller/ebooks`
    

## Admin

- `GET /admin/dashboard`
    
- `GET /admin/ebooks/pending`
    
- `POST /admin/ebooks/{id}/approve`
    
- `POST /admin/ebooks/{id}/reject`
    

---

# 14. Công nghệ đề xuất cho Sprint 1

## Backend

- Spring Boot
    
- Spring MVC
    
- Spring Security
    
- Spring Data JPA
    
- Hibernate Validator
    

## View

- Thymeleaf
    
- Bootstrap hoặc Tailwind CDN
    

## Database

- MySQL hoặc PostgreSQL
    

## File storage

- Local storage trong giai đoạn đầu
    

---

# 15. Điểm mạnh của design này trong Sprint 1

- đơn giản, đúng hướng **monolith SSR**
    
- dễ demo
    
- dễ phát triển tiếp Sprint 2
    
- không over-engineering
    
- role và moderation tách rõ
    

---

# 16. Hạn chế hiện tại

- file storage còn đơn giản
    
- chưa có payment
    
- chưa có library/read access
    
- chưa có preview
    
- chưa có cache strategy chi tiết
    
- chưa tối ưu cho scale lớn
    

---

# 17. Hướng mở rộng sang Sprint 2

Sprint 2 sẽ thêm các khối:

- **Order Module**
    
- **Payment Module**
    
- **Library Module**
    
- **Reader Module**
    

Sơ đồ sẽ mở rộng từ catalog sang:  
**Detail → Order → Payment → Access → Read**

---

Nếu bạn muốn, mình có thể làm tiếp ngay một trong 3 thứ sau:

1. **System Design diagram cho Sprint 2**
    
2. **Class diagram cho Sprint 1 (User, Ebook, Service, Controller)**
    
3. **Deployment diagram (Spring Boot + DB + storage)**