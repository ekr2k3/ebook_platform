Dưới đây là **System Design diagram cho Sprint 2** theo hướng:

- **Spring Boot Monolith**
    
- **SSR**
    
- mở rộng từ Sprint 1
    
- bổ sung:
    
    - Order
        
    - Payment
        
    - Library
        
    - Reader
        

Sprint 2 tập trung hoàn thiện flow:

> **Search → Detail → Buy → Payment → Library → Read**

---

# 1. Kiến trúc tổng thể Sprint 2

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
    E --> C
    C --> G
    G --> A
```

---

# 2. So sánh Sprint 1 và Sprint 2

## Sprint 1 đã có

- Auth
    
- Seller upload
    
- Admin duyệt
    
- Catalog public
    

## Sprint 2 bổ sung

- tạo đơn hàng
    
- thanh toán
    
- callback payment
    
- cấp quyền sở hữu ebook
    
- thư viện cá nhân
    
- reader online
    

---

# 3. Module nội bộ Sprint 2

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
    end

    A5 --> A7
    A7 --> A8
    A8 --> A9
    A9 --> A10
    A3 --> A9
    A3 --> A10
    A6 --> A10
```

---

# 4. Database design Sprint 2

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
```

---

# 5. Ý nghĩa các bảng mới

## `orders`

Lưu thông tin đơn hàng:

- ai mua
    
- tổng tiền
    
- trạng thái thanh toán
    

## `order_items`

Mỗi đơn có thể có một hoặc nhiều ebook.

## `user_ebooks`

Là bảng rất quan trọng trong Sprint 2.  
Đây là bảng xác định:

> user nào đã sở hữu ebook nào

Reader và thư viện sẽ dựa vào bảng này.

---

# 6. Luồng hệ thống Sprint 2

```mermaid
flowchart TD
    A[Buyer mở ebook detail] --> B[Nhấn Mua]
    B --> C[Tạo Order PENDING]
    C --> D[Chuyển tới Payment Gateway]
    D --> E[Gateway callback về hệ thống]
    E --> F[Update Order SUCCESS]
    F --> G[Insert USER_EBOOKS]
    G --> H[Hiển thị trong Library]
    H --> I[Buyer mở Reader]
```

---

# 7. Package/module gợi ý Sprint 2

```mermaid
flowchart TD
    A[com.example.ebookplatform]
    A --> B[controller]
    A --> C[service]
    A --> D[repository]
    A --> E[entity]
    A --> F[dto]
    A --> G[payment]
    A --> H[security]

    B --> B1[OrderController]
    B --> B2[PaymentController]
    B --> B3[LibraryController]
    B --> B4[ReaderController]

    C --> C1[OrderService]
    C --> C2[PaymentService]
    C --> C3[LibraryService]
    C --> C4[ReaderService]

    D --> D1[OrderRepository]
    D --> D2[OrderItemRepository]
    D --> D3[UserEbookRepository]

    E --> E1[Order]
    E --> E2[OrderItem]
    E --> E3[UserEbook]
```

---

# 8. Sequence diagram – Mua ebook

```mermaid
sequenceDiagram
    participant B as Buyer
    participant DC as DetailController
    participant OC as OrderController
    participant OS as OrderService
    participant OR as OrderRepository
    participant DB as Database

    B->>DC: Click "Mua ebook"
    DC->>OC: POST /orders/create
    OC->>OS: createOrder(userId, ebookId)
    OS->>OR: save(order PENDING)
    OR->>DB: INSERT order
    DB-->>OR: success
    OR-->>OS: order
    OS-->>OC: orderId
    OC-->>B: redirect payment page
```

---

# 9. Sequence diagram – Thanh toán và callback

```mermaid
sequenceDiagram
    participant B as Buyer
    participant PC as PaymentController
    participant PS as PaymentService
    participant PG as Payment Gateway
    participant OR as OrderRepository
    participant UR as UserEbookRepository
    participant DB as Database

    B->>PC: Pay order
    PC->>PS: createPayment(orderId)
    PS->>PG: request payment url
    PG-->>PS: payment redirect url
    PS-->>PC: payment url
    PC-->>B: redirect to gateway

    PG-->>PC: callback /payment/callback
    PC->>PS: handleCallback(payload)
    PS->>OR: find order
    OR->>DB: SELECT order
    DB-->>OR: order
    PS->>OR: update status SUCCESS
    OR->>DB: UPDATE order
    PS->>UR: grant ebook to user
    UR->>DB: INSERT user_ebooks
    DB-->>UR: success
    PS-->>PC: payment success
    PC-->>B: redirect /library
```

---

# 10. Sequence diagram – Thư viện cá nhân

```mermaid
sequenceDiagram
    participant B as Buyer
    participant LC as LibraryController
    participant LS as LibraryService
    participant UR as UserEbookRepository
    participant DB as Database

    B->>LC: GET /library
    LC->>LS: getLibrary(userId)
    LS->>UR: findByUserId(userId)
    UR->>DB: SELECT user_ebooks JOIN ebooks
    DB-->>UR: ebook list
    UR-->>LS: library list
    LS-->>LC: library list
    LC-->>B: SSR library page
```

---

# 11. Sequence diagram – Reader online

```mermaid
sequenceDiagram
    participant B as Buyer
    participant RC as ReaderController
    participant RS as ReaderService
    participant UR as UserEbookRepository
    participant ER as EbookRepository
    participant FS as FileStorageService
    participant DB as Database

    B->>RC: GET /reader/{ebookId}
    RC->>RS: openBook(userId, ebookId)
    RS->>UR: existsByUserIdAndEbookId()
    UR->>DB: SELECT ownership
    DB-->>UR: ownership exists
    RS->>ER: findEbookById()
    ER->>DB: SELECT ebook
    DB-->>ER: ebook
    RS->>FS: load file stream
    FS-->>RS: file resource
    RS-->>RC: reader model
    RC-->>B: SSR reader page
```

---

# 12. Reader access control

```mermaid
flowchart TD
    A[Buyer mở /reader/{ebookId}] --> B{Đăng nhập?}
    B -- No --> C[Redirect login]
    B -- Yes --> D{Có quyền sở hữu ebook?}
    D -- No --> E[403 Forbidden / Redirect detail]
    D -- Yes --> F[Load ebook resource]
    F --> G[Render reader page]
```

---

# 13. Payment state machine

```mermaid
stateDiagram-v2
    [*] --> PENDING
    PENDING --> SUCCESS : payment confirmed
    PENDING --> FAILED : payment failed
    PENDING --> CANCELLED : user cancelled
    SUCCESS --> [*]
    FAILED --> [*]
    CANCELLED --> [*]
```

Đây là phần rất quan trọng để tránh lỗi nghiệp vụ.

---

# 14. Route map Sprint 2

## Buyer

- `GET /ebooks/{id}`
    
- `POST /orders/create`
    
- `GET /payment/{orderId}`
    
- `GET /payment/callback`
    
- `GET /library`
    
- `GET /reader/{ebookId}`
    

## Admin

- `GET /admin/orders`
    

## Nội bộ / service

- payment service gọi gateway sandbox
    

---

# 15. Thành phần giao diện SSR cần có trong Sprint 2

## Buyer pages

- ebook detail page
    
- order confirmation page
    
- payment status page
    
- library page
    
- reader page
    

## Admin pages

- orders list page
    

---

# 16. Class/module relationship

```mermaid
classDiagram
    class User {
        Long id
        String email
        String password
        Role role
    }

    class Ebook {
        Long id
        String title
        BigDecimal price
        String fileUrl
        EbookStatus status
    }

    class Order {
        Long id
        BigDecimal totalAmount
        OrderStatus status
        String paymentRef
    }

    class OrderItem {
        Long id
        BigDecimal price
    }

    class UserEbook {
        Long id
        LocalDateTime purchasedAt
    }

    User "1" --> "many" Order
    User "1" --> "many" UserEbook
    User "1" --> "many" Ebook : uploads
    Order "1" --> "many" OrderItem
    Ebook "1" --> "many" OrderItem
    Ebook "1" --> "many" UserEbook
```

---

# 17. Luồng request tổng quát Sprint 2

```mermaid
flowchart TD
    A[Buyer Browser] --> B[Spring Boot Controller]
    B --> C[Spring Security]

    C --> D{Authenticated?}
    D -- No --> E[Redirect login]
    D -- Yes --> F[Service Layer]

    F --> G[Order Service]
    F --> H[Payment Service]
    F --> I[Library Service]
    F --> J[Reader Service]

    G --> K[Repositories]
    H --> K
    I --> K
    J --> K

    K --> L[(Database)]
    J --> M[File Storage]
    H --> N[Payment Gateway]
```

---

# 18. Điểm cần chú ý trong design Sprint 2

## 1. Payment không được xử lý trực tiếp ở browser

Luôn phải:

- tạo order ở backend
    
- backend xác nhận callback
    
- backend cập nhật trạng thái
    

## 2. Reader không nên để file public

Không nên để buyer truy cập trực tiếp file URL nếu muốn kiểm soát quyền tốt hơn.

## 3. `user_ebooks` là trung tâm của Library + Reader

Bảng này quyết định quyền sở hữu.

## 4. Cần idempotency cho callback payment

Nếu gateway gọi callback nhiều lần:

- không được grant ebook lặp lại
    
- không được update order sai
    

---

# 19. Hạn chế hiện tại của Sprint 2

- chưa có DRM mạnh
    
- reader còn cơ bản
    
- chưa sync tiến độ đọc
    
- chưa có refund / chargeback
    
- payment mới ở mức sandbox / mô phỏng
    

---

# 20. Hướng mở rộng sang Sprint 3

Sprint 3 sẽ thêm:

- preview ebook
    
- protection / watermark
    
- seller dashboard
    
- admin user management
    
- takedown
    
- tối ưu search / cache
    

---

Nếu bạn muốn, mình có thể làm tiếp một trong 3 phần rất hữu ích sau:

1. **Deployment diagram Sprint 2**
    
2. **Sequence diagram payment flow bản chi tiết hơn**
    
3. **System Design diagram Sprint 3**