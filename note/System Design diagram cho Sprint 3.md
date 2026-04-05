
Dưới đây là **System Design diagram Sprint 3** cho hệ thống ebook theo hướng:

- **Spring Boot Monolith**
    
- **SSR**
    
- mở rộng từ Sprint 1 + Sprint 2
    
- bổ sung cho Sprint 3:
    
    - preview ebook
        
    - content protection cơ bản
        
    - seller dashboard
        
    - admin user management
        
    - takedown ebook
        
    - tối ưu search / cache public-private
        

---

# 1. Kiến trúc tổng thể Sprint 3

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
        P[Payment Integration Service]
        Q[Preview / Protection Service]
        R[Analytics / Dashboard Service]
        S[System Settings Service]
    end

    B --> I[(MySQL / PostgreSQL)]
    H --> J[(Local Storage / File Server)]
    P --> K[(Payment Gateway Sandbox)]

    C --> D
    D --> F
    F --> I
    D --> H
    H --> J
    D --> P
    P --> K
    D --> Q
    D --> R
    D --> S
    E --> C
    C --> G
    G --> A
```

---

# 2. Mở rộng từ Sprint 2 sang Sprint 3

## Sprint 2 đã có

- catalog
    
- order
    
- payment
    
- library
    
- reader
    

## Sprint 3 bổ sung

- preview trước mua
    
- seller preview trước publish
    
- protection type: NONE / WATERMARK / RESTRICTED_ACCESS
    
- seller dashboard doanh thu
    
- admin quản lý user
    
- admin takedown ebook
    
- tối ưu search, pagination, cache policy
    

---

# 3. Module nội bộ Sprint 3

```mermaid
flowchart LR
    subgraph Spring Boot Monolith
        A1[Auth Module]
        A2[User Module]
        A3[Ebook Module]
        A4[Admin Moderation Module]
        A5[Public Catalog Module]
        A6[File Storage Module]
        A7[Order Module]
        A8[Payment Module]
        A9[Library Module]
        A10[Reader Module]
        A11[Preview Module]
        A12[Protection Module]
        A13[Seller Dashboard Module]
        A14[Admin User Management Module]
        A15[Settings Module]
        A16[Search Optimization Module]
    end

    A3 --> A11
    A3 --> A12
    A12 --> A10
    A11 --> A5
    A7 --> A13
    A8 --> A13
    A14 --> A1
    A4 --> A3
    A15 --> A12
    A15 --> A11
    A16 --> A5
```

---

# 4. Database design Sprint 3

```mermaid
erDiagram
    USERS ||--o{ EBOOKS : uploads
    USERS ||--o{ ORDERS : places
    ORDERS ||--o{ ORDER_ITEMS : contains
    USERS ||--o{ USER_EBOOKS : owns
    EBOOKS ||--o{ ORDER_ITEMS : ordered
    EBOOKS ||--o{ USER_EBOOKS : granted

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
        varchar protection_type
        varchar sample_file_url
        bigint upload_by FK
        timestamp created_at
        timestamp updated_at
    }

    ORDERS {
        bigint id PK
        bigint user_id FK
        decimal total_amount
        varchar status
        varchar payment_ref
        timestamp created_at
        timestamp updated_at
    }

    ORDER_ITEMS {
        bigint id PK
        bigint order_id FK
        bigint ebook_id FK
        decimal price
    }

    USER_EBOOKS {
        bigint id PK
        bigint user_id FK
        bigint ebook_id FK
        timestamp purchased_at
    }

    SYSTEM_SETTINGS {
        bigint id PK
        varchar setting_key
        varchar setting_value
    }
```

---

# 5. Ý nghĩa phần mở rộng DB ở Sprint 3

## `ebooks.protection_type`

Xác định chế độ bảo vệ:

- `NONE`
    
- `WATERMARK`
    
- `RESTRICTED_ACCESS`
    

## `ebooks.sample_file_url`

Lưu file preview/sample để buyer xem trước.

## `ebooks.status`

Mở rộng thêm:

- `PENDING`
    
- `APPROVED`
    
- `REJECTED`
    
- `TAKEDOWN`
    

## `system_settings`

Cho admin cấu hình:

- default protection type
    
- enable preview
    
- watermark pattern
    

---

# 6. Luồng hệ thống tổng quát Sprint 3

```mermaid
flowchart TD
    A[Seller upload ebook] --> B[Chọn protection type]
    B --> C[Admin duyệt ebook]
    C --> D[Ebook được publish]

    D --> E[Buyer xem detail]
    E --> F[Buyer xem preview]
    E --> G[Buyer mua ebook]
    G --> H[Payment success]
    H --> I[Grant ownership]
    I --> J[Buyer vào library]
    J --> K[Buyer mở reader]

    K --> L{Protection type}
    L -- NONE --> M[Read normally]
    L -- WATERMARK --> N[Read with watermark info]
    L -- RESTRICTED_ACCESS --> O[Read with ownership check]
```

---

# 7. Sequence diagram – Buyer xem preview ebook

```mermaid
sequenceDiagram
    participant B as Buyer
    participant PC as PreviewController
    participant PS as PreviewService
    participant ER as EbookRepository
    participant FS as FileStorageService
    participant DB as Database

    B->>PC: GET /ebooks/{id}/preview
    PC->>PS: getPreview(ebookId)
    PS->>ER: findApprovedEbook(ebookId)
    ER->>DB: SELECT ebook WHERE status='APPROVED'
    DB-->>ER: ebook
    ER-->>PS: ebook
    PS->>FS: load sample file
    FS-->>PS: sample resource
    PS-->>PC: preview model/resource
    PC-->>B: SSR preview page
```

---

# 8. Sequence diagram – Seller preview trước publish

```mermaid
sequenceDiagram
    participant S as Seller
    participant SC as SellerPreviewController
    participant SS as EbookService
    participant ER as EbookRepository
    participant FS as FileStorageService
    participant DB as Database

    S->>SC: GET /seller/ebooks/{id}/preview
    SC->>SS: previewOwnEbook(sellerId, ebookId)
    SS->>ER: findByIdAndSeller(ebookId, sellerId)
    ER->>DB: SELECT ebook by id and seller
    DB-->>ER: ebook
    ER-->>SS: ebook
    SS->>FS: load uploaded file
    FS-->>SS: file resource
    SS-->>SC: preview resource
    SC-->>S: SSR preview page
```

---

# 9. Sequence diagram – Reader với protection

```mermaid
sequenceDiagram
    participant B as Buyer
    participant RC as ReaderController
    participant RS as ReaderService
    participant PS as ProtectionService
    participant UR as UserEbookRepository
    participant ER as EbookRepository
    participant FS as FileStorageService
    participant DB as Database

    B->>RC: GET /reader/{ebookId}
    RC->>RS: openBook(userId, ebookId)
    RS->>UR: check ownership
    UR->>DB: SELECT ownership
    DB-->>UR: ownership exists
    RS->>ER: find ebook
    ER->>DB: SELECT ebook
    DB-->>ER: ebook
    RS->>PS: applyProtection(user, ebook)
    PS-->>RS: read policy
    RS->>FS: load file/resource
    FS-->>RS: file resource
    RS-->>RC: reader model
    RC-->>B: SSR reader page
```

---

# 10. Protection flow

```mermaid
flowchart TD
    A[User mở reader] --> B{Có ownership?}
    B -- No --> C[403 Forbidden]
    B -- Yes --> D{Protection type}

    D -- NONE --> E[Render normally]
    D -- WATERMARK --> F[Inject watermark info]
    D -- RESTRICTED_ACCESS --> G[Render only through protected route]
    F --> H[SSR Reader Page]
    G --> H
    E --> H
```

---

# 11. Sequence diagram – Seller dashboard doanh thu

```mermaid
sequenceDiagram
    participant S as Seller
    participant DC as SellerDashboardController
    participant DS as SellerDashboardService
    participant OR as OrderRepository
    participant ER as EbookRepository
    participant DB as Database

    S->>DC: GET /seller/dashboard
    DC->>DS: getDashboard(sellerId)
    DS->>ER: count ebooks by seller
    ER->>DB: SELECT count(*)
    DB-->>ER: ebook stats
    DS->>OR: sum successful order items by seller
    OR->>DB: SELECT revenue stats
    DB-->>OR: revenue stats
    DS-->>DC: dashboard data
    DC-->>S: SSR dashboard page
```

---

# 12. Sequence diagram – Admin takedown ebook

```mermaid
sequenceDiagram
    participant A as Admin
    participant AC as AdminModerationController
    participant AS as AdminModerationService
    participant ER as EbookRepository
    participant DB as Database

    A->>AC: POST /admin/ebooks/{id}/takedown
    AC->>AS: takedownEbook(id, reason)
    AS->>ER: findById(id)
    ER->>DB: SELECT ebook
    DB-->>ER: ebook
    ER-->>AS: ebook
    AS->>ER: update status = TAKEDOWN
    ER->>DB: UPDATE ebooks
    DB-->>ER: success
    AS-->>AC: success
    AC-->>A: redirect moderation page
```

---

# 13. Sequence diagram – Admin quản lý user

```mermaid
sequenceDiagram
    participant A as Admin
    participant UC as AdminUserController
    participant US as UserManagementService
    participant UR as UserRepository
    participant DB as Database

    A->>UC: POST /admin/users/{id}/block
    UC->>US: blockUser(id)
    US->>UR: findById(id)
    UR->>DB: SELECT user
    DB-->>UR: user
    UR-->>US: user
    US->>UR: save(status=INACTIVE)
    UR->>DB: UPDATE users
    DB-->>UR: success
    US-->>UC: success
    UC-->>A: redirect users page
```

---

# 14. Search optimization flow

```mermaid
flowchart TD
    A[Buyer search keyword] --> B[PublicCatalogController]
    B --> C[SearchService]
    C --> D[Repository with Pageable]
    D --> E[(Database)]
    E --> F[Approved ebooks only]
    F --> G[Paginated result]
    G --> H[SSR results page]
```

---

# 15. Cache policy design cho SSR

```mermaid
flowchart TD
    A[HTTP Request] --> B{Page Type?}

    B -- Public Catalog / Detail / Preview --> C[Allow cache policy]
    B -- Library / Reader / Seller Dashboard / Admin --> D[No-store private policy]

    C --> E[Cache-Control public, max-age]
    D --> F[Cache-Control no-store]
```

---

# 16. Kiến trúc request tổng quát Sprint 3

```mermaid
flowchart TD
    A[Browser] --> B[Spring Boot Controller]
    B --> C[Spring Security]

    C --> D{Authenticated?}
    D -- No --> E[Public routes only]
    D -- Yes --> F[Role-based routing]

    F --> G[Preview Service]
    F --> H[Reader Service]
    F --> I[Seller Dashboard Service]
    F --> J[Admin User Management Service]
    F --> K[Moderation Service]
    F --> L[Search Service]

    G --> M[Repository]
    H --> M
    I --> M
    J --> M
    K --> M
    L --> M

    M --> N[(Database)]
    G --> O[File Storage]
    H --> O
```

---

# 17. Route map Sprint 3

## Public / Buyer

- `GET /ebooks/{id}/preview`
    
- `GET /search?page=...`
    
- `GET /reader/{ebookId}`
    

## Seller

- `GET /seller/ebooks/{id}/preview`
    
- `GET /seller/dashboard`
    

## Admin

- `GET /admin/users`
    
- `POST /admin/users/{id}/block`
    
- `POST /admin/users/{id}/unblock`
    
- `POST /admin/ebooks/{id}/takedown`
    
- `GET /admin/settings`
    
- `POST /admin/settings`
    

---

# 18. Class relationship mở rộng Sprint 3

```mermaid
classDiagram
    class User {
        Long id
        String email
        String role
        String status
    }

    class Ebook {
        Long id
        String title
        BigDecimal price
        String fileUrl
        String sampleFileUrl
        String protectionType
        String status
    }

    class Order {
        Long id
        BigDecimal totalAmount
        String status
    }

    class UserEbook {
        Long id
        LocalDateTime purchasedAt
    }

    class SystemSetting {
        Long id
        String settingKey
        String settingValue
    }

    User "1" --> "many" Ebook : uploads
    User "1" --> "many" Order
    User "1" --> "many" UserEbook
    Ebook "1" --> "many" UserEbook
    SystemSetting ..> Ebook : default protection config
```

---

# 19. Điểm mạnh của design Sprint 3

- vẫn giữ đúng hướng **monolith SSR**
    
- tách module rõ nhưng chưa cần microservices
    
- dễ demo MVP hoàn chỉnh
    
- có khả năng kiểm soát nội dung tốt hơn
    
- seller/admin bắt đầu có dashboard quản trị thực tế
    

---

# 20. Hạn chế hiện tại của Sprint 3

- watermark mới ở mức cơ bản
    
- chưa có DRM mạnh thực sự
    
- dashboard analytics còn đơn giản
    
- chưa có async job cho preview generation lớn
    
- chưa có search engine riêng như Elasticsearch
    
- chưa có CDN / object storage chuẩn production
    

---

# 21. Hướng sau Sprint 3

Sau Sprint 3 có thể mở rộng theo 3 nhánh:

## A. Production hardening

- object storage
    
- CDN
    
- async processing
    
- audit log
    

## B. Product features

- review/rating
    
- sync tiến độ đọc
    
- bookmark/highlight
    
- recommendation
    

## C. Security/DRM

- watermark nâng cao
    
- anti-sharing
    
- device limit
    
- license-based access
    

---

Nếu bạn muốn, mình có thể làm tiếp một trong các phần sau để nối trọn bộ:

**1. Deployment diagram tổng thể cho 3 sprint**  
**2. Sequence diagram chi tiết luồng “Buyer mua và đọc ebook”**  
**3. Class diagram tổng thể toàn hệ thống Spring Boot ebook**