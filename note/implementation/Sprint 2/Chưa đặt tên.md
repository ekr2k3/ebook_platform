Dưới đây là **implement Sprint 2 chi tiết theo Spring Boot monolith SSR**, nối tiếp trực tiếp từ Sprint 1.

Phạm vi Sprint 2:

- tạo đơn hàng
    
- thanh toán
    
- callback payment
    
- cấp quyền sở hữu ebook
    
- thư viện cá nhân
    
- reader online
    
- admin xem giao dịch
    

Mình sẽ giữ cùng style với Sprint 1 để bạn ghép vào tài liệu cho đồng bộ.

---

# 1. Mục tiêu Sprint 2

Sau Sprint 2, hệ thống phải chạy được flow:

**Buyer tìm sách → xem chi tiết → mua → thanh toán thành công → ebook vào thư viện → đọc online**

---

# 2. Cấu trúc package bổ sung

```text
src/main/java/com/example/ebook
├── controller
│   ├── OrderController.java
│   ├── PaymentController.java
│   ├── LibraryController.java
│   ├── ReaderController.java
│   └── AdminOrderController.java
├── dto
│   ├── CreateOrderRequest.java
│   └── PaymentCallbackRequest.java
├── entity
│   ├── Order.java
│   ├── OrderItem.java
│   └── UserEbook.java
├── enums
│   └── OrderStatus.java
├── repository
│   ├── OrderRepository.java
│   ├── OrderItemRepository.java
│   └── UserEbookRepository.java
├── service
│   ├── OrderService.java
│   ├── PaymentService.java
│   ├── LibraryService.java
│   └── ReaderService.java
```

---

# 3. Enum mới

## OrderStatus.java

```java
package com.example.ebook.enums;

public enum OrderStatus {
    PENDING, SUCCESS, FAILED, CANCELLED
}
```

---

# 4. Entity mới

## Order.java

```java
package com.example.ebook.entity;

import com.example.ebook.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "payment_ref")
    private String paymentRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User buyer;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

## OrderItem.java

```java
package com.example.ebook.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ebook_id", nullable = false)
    private Ebook ebook;
}
```

## UserEbook.java

```java
package com.example.ebook.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_ebooks",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "ebook_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEbook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ebook_id", nullable = false)
    private Ebook ebook;

    @Column(name = "purchased_at")
    private LocalDateTime purchasedAt;
}
```

---

# 5. Repository mới

## OrderRepository.java

```java
package com.example.ebook.repository;

import com.example.ebook.entity.Order;
import com.example.ebook.entity.User;
import com.example.ebook.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBuyer(User buyer);

    List<Order> findByStatus(OrderStatus status);

    Optional<Order> findByPaymentRef(String paymentRef);
}
```

## OrderItemRepository.java

```java
package com.example.ebook.repository;

import com.example.ebook.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
```

## UserEbookRepository.java

```java
package com.example.ebook.repository;

import com.example.ebook.entity.Ebook;
import com.example.ebook.entity.User;
import com.example.ebook.entity.UserEbook;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserEbookRepository extends JpaRepository<UserEbook, Long> {

    List<UserEbook> findByBuyer(User buyer);

    boolean existsByBuyerAndEbook(User buyer, Ebook ebook);

    Optional<UserEbook> findByBuyerAndEbook(User buyer, Ebook ebook);
}
```

---

# 6. DTO mới

## CreateOrderRequest.java

```java
package com.example.ebook.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderRequest {

    @NotNull
    private Long ebookId;
}
```

## PaymentCallbackRequest.java

```java
package com.example.ebook.dto;

import lombok.Data;

@Data
public class PaymentCallbackRequest {
    private String paymentRef;
    private String status;
}
```

---

# 7. OrderService

## OrderService.java

```java
package com.example.ebook.service;

import com.example.ebook.entity.Ebook;
import com.example.ebook.entity.Order;
import com.example.ebook.entity.OrderItem;
import com.example.ebook.entity.User;
import com.example.ebook.enums.EbookStatus;
import com.example.ebook.enums.OrderStatus;
import com.example.ebook.repository.EbookRepository;
import com.example.ebook.repository.OrderRepository;
import com.example.ebook.repository.UserEbookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final EbookRepository ebookRepository;
    private final OrderRepository orderRepository;
    private final UserEbookRepository userEbookRepository;

    @Transactional
    public Order createOrder(Long ebookId, User buyer) {
        Ebook ebook = ebookRepository.findByIdAndStatus(ebookId, EbookStatus.APPROVED)
                .orElseThrow(() -> new RuntimeException("Ebook không tồn tại hoặc chưa được duyệt"));

        if (userEbookRepository.existsByBuyerAndEbook(buyer, ebook)) {
            throw new RuntimeException("Bạn đã sở hữu ebook này");
        }

        Order order = Order.builder()
                .buyer(buyer)
                .totalAmount(ebook.getPrice())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .ebook(ebook)
                .price(ebook.getPrice())
                .build();

        order.setItems(List.of(item));
        return orderRepository.save(order);
    }

    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order không tồn tại"));
    }

    public List<Order> getOrdersForAdmin() {
        return orderRepository.findAll();
    }
}
```

---

# 8. PaymentService

Ở Sprint 2, bạn có thể làm theo 2 cách:

- **Mock payment**
    
- Tích hợp sandbox như VNPay / Stripe
    

Để dễ đồ án, mình khuyên làm **mock payment trước**, vì focus là nghiệp vụ.

## PaymentService.java

```java
package com.example.ebook.service;

import com.example.ebook.entity.Ebook;
import com.example.ebook.entity.Order;
import com.example.ebook.entity.UserEbook;
import com.example.ebook.enums.OrderStatus;
import com.example.ebook.repository.OrderRepository;
import com.example.ebook.repository.UserEbookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final UserEbookRepository userEbookRepository;

    public String createMockPayment(Order order) {
        String paymentRef = UUID.randomUUID().toString();
        order.setPaymentRef(paymentRef);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        return "/payment/mock-checkout?paymentRef=" + paymentRef;
    }

    @Transactional
    public void handleSuccess(String paymentRef) {
        Order order = orderRepository.findByPaymentRef(paymentRef)
                .orElseThrow(() -> new RuntimeException("Payment reference không hợp lệ"));

        if (order.getStatus() == OrderStatus.SUCCESS) {
            return;
        }

        order.setStatus(OrderStatus.SUCCESS);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);

        Ebook ebook = order.getItems().get(0).getEbook();

        boolean alreadyGranted = userEbookRepository.existsByBuyerAndEbook(order.getBuyer(), ebook);
        if (!alreadyGranted) {
            UserEbook userEbook = UserEbook.builder()
                    .buyer(order.getBuyer())
                    .ebook(ebook)
                    .purchasedAt(LocalDateTime.now())
                    .build();

            userEbookRepository.save(userEbook);
        }
    }

    @Transactional
    public void handleFailure(String paymentRef) {
        Order order = orderRepository.findByPaymentRef(paymentRef)
                .orElseThrow(() -> new RuntimeException("Payment reference không hợp lệ"));

        if (order.getStatus() == OrderStatus.SUCCESS) {
            return;
        }

        order.setStatus(OrderStatus.FAILED);
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
    }
}
```

---

# 9. LibraryService

## LibraryService.java

```java
package com.example.ebook.service;

import com.example.ebook.entity.User;
import com.example.ebook.entity.UserEbook;
import com.example.ebook.repository.UserEbookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LibraryService {

    private final UserEbookRepository userEbookRepository;

    public List<UserEbook> getLibrary(User buyer) {
        return userEbookRepository.findByBuyer(buyer);
    }
}
```

---

# 10. ReaderService

## ReaderService.java

```java
package com.example.ebook.service;

import com.example.ebook.entity.Ebook;
import com.example.ebook.entity.User;
import com.example.ebook.repository.EbookRepository;
import com.example.ebook.repository.UserEbookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReaderService {

    private final EbookRepository ebookRepository;
    private final UserEbookRepository userEbookRepository;

    public Ebook openBook(Long ebookId, User buyer) {
        Ebook ebook = ebookRepository.findById(ebookId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ebook"));

        boolean hasAccess = userEbookRepository.existsByBuyerAndEbook(buyer, ebook);
        if (!hasAccess) {
            throw new RuntimeException("Bạn không có quyền truy cập ebook này");
        }

        return ebook;
    }
}
```

---

# 11. Controller mới

## OrderController.java

```java
package com.example.ebook.controller;

import com.example.ebook.dto.CreateOrderRequest;
import com.example.ebook.entity.Order;
import com.example.ebook.entity.User;
import com.example.ebook.security.CustomUserDetails;
import com.example.ebook.service.OrderService;
import com.example.ebook.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    @PostMapping("/create")
    public String createOrder(@Valid @ModelAttribute CreateOrderRequest request,
                              @AuthenticationPrincipal CustomUserDetails userDetails) {
        User buyer = userDetails.getUser();
        Order order = orderService.createOrder(request.getEbookId(), buyer);

        String paymentUrl = paymentService.createMockPayment(order);
        return "redirect:" + paymentUrl;
    }
}
```

## PaymentController.java

```java
package com.example.ebook.controller;

import com.example.ebook.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/payment")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/mock-checkout")
    public String mockCheckout(@RequestParam String paymentRef, Model model) {
        model.addAttribute("paymentRef", paymentRef);
        return "payment/mock-checkout";
    }

    @PostMapping("/mock-success")
    public String mockSuccess(@RequestParam String paymentRef) {
        paymentService.handleSuccess(paymentRef);
        return "redirect:/library?paymentSuccess";
    }

    @PostMapping("/mock-fail")
    public String mockFail(@RequestParam String paymentRef) {
        paymentService.handleFailure(paymentRef);
        return "redirect:/?paymentFailed";
    }
}
```

## LibraryController.java

```java
package com.example.ebook.controller;

import com.example.ebook.entity.User;
import com.example.ebook.security.CustomUserDetails;
import com.example.ebook.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;

    @GetMapping("/library")
    public String library(@AuthenticationPrincipal CustomUserDetails userDetails,
                          Model model) {
        User buyer = userDetails.getUser();
        model.addAttribute("library", libraryService.getLibrary(buyer));
        return "buyer/library";
    }
}
```

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
        Ebook ebook = readerService.openBook(ebookId, buyer);
        model.addAttribute("ebook", ebook);
        return "buyer/reader";
    }
}
```

## AdminOrderController.java

```java
package com.example.ebook.controller;

import com.example.ebook.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public String orderList(Model model) {
        model.addAttribute("orders", orderService.getOrdersForAdmin());
        return "admin/orders";
    }
}
```

---

# 12. Update SecurityConfig cho Sprint 2

Thêm các route mới.

```java
.requestMatchers("/", "/register", "/login", "/ebooks/**", "/search", "/css/**", "/js/**").permitAll()
.requestMatchers("/seller/**").hasRole("SELLER")
.requestMatchers("/admin/**").hasRole("ADMIN")
.requestMatchers("/orders/**", "/payment/**", "/library/**", "/reader/**").hasRole("BUYER")
.anyRequest().authenticated()
```

Nếu bạn muốn public preview detail page ở sprint sau thì để sau.

---

# 13. Update trang detail để mua ebook

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

<form th:action="@{/orders/create}" method="post">
    <input type="hidden" name="ebookId" th:value="${ebook.id}" />
    <button type="submit">Mua ebook</button>
</form>
</body>
</html>
```

---

# 14. Thymeleaf page mới

## `templates/payment/mock-checkout.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Mock Payment</title>
</head>
<body>
<h2>Thanh toán mô phỏng</h2>
<p>Payment Ref: <span th:text="${paymentRef}"></span></p>

<form th:action="@{/payment/mock-success}" method="post" style="display:inline;">
    <input type="hidden" name="paymentRef" th:value="${paymentRef}" />
    <button type="submit">Thanh toán thành công</button>
</form>

<form th:action="@{/payment/mock-fail}" method="post" style="display:inline;">
    <input type="hidden" name="paymentRef" th:value="${paymentRef}" />
    <button type="submit">Thanh toán thất bại</button>
</form>
</body>
</html>
```

## `templates/buyer/library.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Thư viện của tôi</title>
</head>
<body>
<h1>Thư viện của tôi</h1>

<div th:if="${#lists.isEmpty(library)}">
    <p>Bạn chưa có ebook nào.</p>
</div>

<div th:each="item : ${library}">
    <h3 th:text="${item.ebook.title}"></h3>
    <p th:text="${item.ebook.author}"></p>
    <a th:href="@{'/reader/' + ${item.ebook.id}}">Đọc ngay</a>
</div>
</body>
</html>
```

## `templates/buyer/reader.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Reader</title>
</head>
<body>
<h1 th:text="${ebook.title}"></h1>
<p><strong>Tác giả:</strong> <span th:text="${ebook.author}"></span></p>
<p>File: <span th:text="${ebook.fileUrl}"></span></p>

<p>Ở Sprint 2, đây là reader cơ bản. Có thể tích hợp PDF.js ở bước sau.</p>
</body>
</html>
```

## `templates/admin/orders.html`

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Quản lý đơn hàng</title>
</head>
<body>
<h1>Danh sách đơn hàng</h1>

<table border="1">
    <tr>
        <th>ID</th>
        <th>Buyer</th>
        <th>Tổng tiền</th>
        <th>Trạng thái</th>
        <th>Payment Ref</th>
    </tr>

    <tr th:each="order : ${orders}">
        <td th:text="${order.id}"></td>
        <td th:text="${order.buyer.email}"></td>
        <td th:text="${order.totalAmount}"></td>
        <td th:text="${order.status}"></td>
        <td th:text="${order.paymentRef}"></td>
    </tr>
</table>
</body>
</html>
```

---

# 15. Triển khai reader cơ bản ở Sprint 2

Hiện tại có 2 mức:

## Mức đơn giản để đồ án chạy được

- chỉ render metadata
    
- có link file/path
    
- chứng minh quyền truy cập
    

## Mức tốt hơn

- tích hợp PDF.js để mở PDF
    
- EPUB thì hiển thị như download hoặc parse đơn giản
    

Nếu bạn cần, ở bước sau mình có thể viết luôn cách tích hợp **PDF.js với Spring Boot**.

---

# 16. Luồng nghiệp vụ chính

## Buyer mua ebook

1. Buyer vào detail
    
2. Nhấn mua
    
3. Tạo order `PENDING`
    
4. Sinh `paymentRef`
    
5. Chuyển đến mock checkout
    
6. Nếu success:
    
    - update order = `SUCCESS`
        
    - thêm vào `user_ebooks`
        
7. Buyer vào thư viện
    
8. Buyer đọc ebook
    

---

# 17. Những gì cần test trong Sprint 2

## Order

- tạo order đúng ebook approved
    
- không mua sách chưa approved
    
- không mua lại sách đã có
    

## Payment

- success cập nhật đúng
    
- fail cập nhật đúng
    
- callback lặp không cấp ebook 2 lần
    

## Library

- chỉ hiển thị sách buyer sở hữu
    
- buyer khác không thấy
    

## Reader

- chỉ user sở hữu ebook mới truy cập được
    
- truy cập trái phép bị chặn
    

## Admin

- xem được tất cả đơn hàng
    

---

# 18. Những bug/rủi ro hay gặp

## Duplicate grant ebook

Nếu callback bị bắn nhiều lần:

- có thể insert trùng `user_ebooks`
    

Giải pháp:

- unique constraint `(user_id, ebook_id)`
    
- check tồn tại trước khi save
    

## Order success nhưng không grant ebook

Nếu save order xong nhưng save `user_ebooks` lỗi:

- data inconsistency
    

Giải pháp:

- `@Transactional` trong `handleSuccess`
    

## Buyer truy cập reader bằng URL tay

Giải pháp:

- luôn check `userEbookRepository.existsByBuyerAndEbook(...)`
    

## Payment mock quá đơn giản

Đúng, nhưng Sprint 2 chủ yếu chứng minh nghiệp vụ.  
Sau đó mới thay bằng gateway thật.

---

# 19. Cải thiện cho Sprint 2.5 hoặc Sprint 3

- refund / chargeback
    
- trạng thái `CANCELLED`
    
- order history cho buyer
    
- payment gateway thật
    
- PDF.js reader
    
- preview ebook
    
- watermark
    

---

# 20. Kết quả sau khi implement xong Sprint 2

Bạn sẽ có:

- flow mua ebook hoàn chỉnh
    
- payment mock chạy end-to-end
    
- cấp quyền ebook chính xác
    
- thư viện cá nhân hoạt động
    
- reader cơ bản hoạt động
    
- admin thấy được đơn hàng
    

---

Nếu bạn muốn, bước tiếp theo hợp lý nhất là một trong 3 cái này:

1. **Implement Sprint 3 chi tiết theo code Spring Boot**
    
2. **Tích hợp PDF.js vào reader Sprint 2**
    
3. **Vẽ class diagram + API list hoàn chỉnh cho toàn hệ thống**
    

Nói `làm sprint 3 code luôn` là mình viết tiếp ngay.