# Kế hoạch Gộp Monorepo & Nâng cấp Codebase Event Ticket System (Java Spring Boot + Angular)

## Bối cảnh & Mục tiêu

- **Repo chính (Monorepo đích):** `dlvanh/event-ticket-system` (Java 21, Spring Boot 3.4.5, MySQL, Hibernate JPA, PayOS, JWT/OAuth2).
- **Repo giao diện gộp vào:** `dlvanh/Online_ticketing_system_FE` (Angular 19.2, Chart.js).
- **Quyết định kiến trúc đã chốt:**
  1. Giữ nguyên **Java 21 + Spring Boot 3.4.5** ở Backend và **Angular 19.2** ở Frontend (chuyển từ SSR sang **SPA thuần** để gọn và tránh lỗi `localStorage`).
  2. Gộp thành 1 Monorepo lấy `event-ticket-system` làm gốc, **giữ nguyên toàn bộ lịch sử commit của Frontend** qua `git subtree` (không `--squash`).
  3. **Bỏ Flyway migration** (`flyway-core`, `flyway-mysql`, `spring.flyway.*`, không tạo `db/migration/`) — dùng **Hibernate JPA** quản lý schema.
  4. **Hoãn các phần deploy production & CI/CD** (không tạo `.github/workflows/`, `application-prod.yml`, `infra/`), nhưng **giữ lại Docker** (`backend/Dockerfile`, `frontend/Dockerfile` kèm `nginx.conf` proxy `/api` → `backend:8080`, `docker-compose.yml`) để Backend, Frontend, MySQL và Redis chạy và giao tiếp trực tiếp với nhau.
  5. **Dọn bỏ code chết/trùng lặp công nghệ** ở Backend (`AccountController`, `Mapper/` + MyBatis, `CheckoutController` + Thymeleaf `templates/`/`static/`).

---

## Kiến trúc Monorepo Mục tiêu

```text
event-ticket-system/
├── backend/                          # Spring Boot backend (package lowercase chuẩn Java)
│   ├── .mvn/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/event_ticket_system/
│   │   │   │   ├── config/           # PayOS, OpenAPI, Redis, Async, CORS
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/              # request/, response/
│   │   │   │   ├── entity/
│   │   │   │   ├── enums/
│   │   │   │   ├── exception/        # GlobalExceptionHandler + domain exceptions
│   │   │   │   ├── repository/
│   │   │   │   ├── security/         # SecurityConfig, JwtFilter, JwtUtil, CustomUserPrincipal
│   │   │   │   ├── service/          # EventCommandService, EventQueryService, EventReportService...
│   │   │   │   ├── util/             # QRCodeUtil (HMAC-SHA256)
│   │   │   │   └── validation/       # Đổi tên từ ValidLogin, bỏ ValidLoginPassword
│   │   │   └── resources/
│   │   │       ├── fonts/            # Chỉ giữ arial.ttf & arialbd.ttf
│   │   │       └── application.yml   # Đọc secrets từ biến môi trường (.env)
│   │   └── test/                     # Unit + Testcontainers Integration Tests
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── pom.xml
│   └── Dockerfile
├── frontend/                         # Angular 19 SPA (từ Online_ticketing_system_FE)
│   ├── public/assets/                # Chỉ giữ assets thực dùng (xóa public/dist & public/plugins)
│   ├── src/
│   │   ├── environments/             # environment.ts (apiUrl: '/api') & environment.development.ts
│   │   └── app/
│   │       ├── core/                 # Auth, guards, token/error interceptors, typed models
│   │       ├── shared/               # EventCarouselComponent, header/footer, pipes
│   │       └── features/             # auth, buyer, organizer, admin (lazy-loaded)
│   ├── angular.json                  # Cấu hình SPA (bỏ SSR)
│   ├── proxy.conf.json               # Proxy /api -> http://localhost:8080 khi chạy ng serve
│   ├── nginx.conf                    # Serve static Angular + proxy /api -> http://backend:8080 trong Docker
│   ├── package.json
│   └── Dockerfile
├── docker-compose.yml                # MySQL 8 + Redis + Backend + Frontend giao tiếp qua Docker network
├── .env.example                      # Mẫu biến môi trường (không commit .env thật)
├── .gitignore
└── README.md
```

---

## Phase 0: Chuẩn hóa Git & Gộp Monorepo (Giữ lịch sử Frontend)

> [!IMPORTANT]
> **Không dùng `--squash`** khi chạy `git subtree add` để bảo toàn 100% commit history của `Online_ticketing_system_FE`. Các file vendor cũ (`public/dist/`, `public/plugins/`) sẽ nằm trong lịch sử cũ và được xóa khỏi working tree ở Phase 4.

### Các bước thực hiện
1. **Tạo nhánh làm việc:** Tạo nhánh `chore/monorepo-setup` từ nhánh hiện tại của `event-ticket-system`.
2. **Di chuyển Backend vào `backend/`:** Dùng `git mv` di chuyển `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn`, `src` vào `backend/` và commit mốc di chuyển.
3. **Gộp Frontend vào `frontend/` giữ nguyên lịch sử commit:**
   ```bash
   git remote add fe https://github.com/dlvanh/Online_ticketing_system_FE.git
   git fetch fe main
   git subtree add --prefix=frontend fe main
   git remote remove fe
   ```
4. **Dựng cấu hình Docker giao tiếp FE ↔ BE:**
   - Cập nhật [`.gitignore`](file:///home/antigravity/dlvanh/event-ticket-system/.gitignore) ở root cho Java/Maven (`backend/target/`, `.idea/`), Node/Angular (`frontend/node_modules/`, `frontend/dist/`, `frontend/.angular/`), và `.env`.
   - Tạo `.env.example` (khai báo `DB_*`, `REDIS_*`, `JWT_SECRET`, `IMGBB_API_KEY`, `MAIL_*`, `PAYOS_*`, `GEMINI_API_KEY`, `GOOGLE_OAUTH_*`, `CORS_ALLOWED_ORIGINS`).
   - Tạo `backend/Dockerfile` (multi-stage Maven + Eclipse Temurin 21 JRE), `frontend/Dockerfile` (Node 22 build Angular SPA + Nginx), `frontend/nginx.conf` (proxy `/api/` sang `http://backend:8080/api/`), và `docker-compose.yml` nối 4 service `mysql`, `redis`, `backend`, `frontend` chung network.
5. **Khóa repo cũ (sau khi merge PR):** Archive `dlvanh/Online_ticketing_system_FE` trên GitHub để team chỉ commit vào 1 nơi duy nhất.

---

## Phase 1: Khóa Bảo mật & Rò rỉ Dữ liệu (Ưu tiên khẩn cấp)

### 1.1. Tách Secrets & Loại bỏ Flyway
- Gỡ toàn bộ credentials đang hardcode trong [`application.properties`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/resources/application.properties#L10-L56) (DB password `root`, `jwt.secret`, `imgbb.api.key`, `spring.mail.password`) → chuyển sang `application.yml` đọc từ biến môi trường (có giá trị fallback an toàn cho local dev qua `.env` / Docker Compose).
- **Xóa cấu hình `spring.flyway.*`** và **gỡ `flyway-core`, `flyway-mysql`** khỏi [`pom.xml`](file:///home/antigravity/dlvanh/event-ticket-system/pom.xml#L72-L87).
- Sửa hardcode email gửi đi trong [`EmailService.java`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Service/EmailService.java#L29) để đọc từ `@Value("${spring.mail.username}")`.
- **Hành động ngoài code bắt buộc:** Thu hồi & tạo mới toàn bộ `spring.mail.password`, `imgbb.api.key`, `jwt.secret` và mật khẩu DB đã từng commit trong Git.

### 1.2. Vá lỗ hổng Spring Security & RBAC
- Trong [`SecurityConfig.java`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Security/SecurityConfig.java):
  - Bật `@EnableMethodSecurity` để `@PreAuthorize` hoạt động thật.
  - Cấu hình `SessionCreationPolicy.STATELESS` cho API JWT (tách filter chain cho `/oauth2/**` nếu giữ Google OAuth2 login).
  - **Xóa** `.requestMatchers("/api/users/**").permitAll()`, phân quyền rõ:
    - Public: `/api/auth/**`, `/api/payment/payos_transfer_handler`, `/api/events/recommend`, `GET /api/events/{eventId:\d+}`, `GET /api/review/event/**`, `/api/provinces/**`, `/api/districts/**`, `/api/wards/**`, `/api/chat`, `/v3/api-docs/**`, `/swagger-ui/**`.
    - Admin (`hasRole("admin")`): `DELETE /api/users/delete`, `DELETE /api/users/disable`, `GET /api/users`, `/api/users/admin/**`, `/api/events/pending`, `GET /api/events`, `/api/orders/list`, `/api/orders/confirm-webhook`, `/api/orders/confirm-order/**`, `/api/orders/cancel-order/**`.
    - Authenticated: hồ sơ cá nhân, đặt vé, đánh giá, quản lý sự kiện của organizer.
  - Hợp nhất CORS về 1 cấu hình duy nhất trong `SecurityConfig` (đọc từ `CORS_ALLOWED_ORIGINS`, hỗ trợ cả `http://localhost:4200` và `http://localhost`), bỏ CORS trùng ở [`WebConfig.java`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Security/WebConfig.java#L30-L37).
- Sửa `@PreAuthorize("hasAnyAuthority('ROLE_user', ...)")` ở [`UserController.updateUserProfile`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Controller/UserController.java#L238) thành `ROLE_customer` khớp [`UserRole`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Enums/UserRole.java).

### 1.3. Chuẩn hóa Principal & Chặn IDOR / Leak Entity
- Tạo `CustomUserPrincipal(Integer userId, String email, UserRole role)` và gán vào `SecurityContext` trong [`JwtFilter.java`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Security/JwtFilter.java#L69) (thay vì `fullName`), kết hợp `SecurityUtils` / `@AuthenticationPrincipal` để bỏ `HttpServletRequest` ở tầng Service.
- **Vá IDOR:** Kiểm tra `event.getOrganizer().getId().equals(currentUserId)` (hoặc `ROLE_admin`) trong [`EventServiceImpl.generateBuyerReportExcel`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Service/Impl/EventServiceImpl.java#L952) và [`generateBuyerReportPdf`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Service/Impl/EventServiceImpl.java#L1034).
- **Vá rò rỉ `passwordHash`:** Thêm `@JsonIgnore` vào `User.passwordHash` ([`User.java`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Entity/User.java#L50)) và trả `DiscountResponseDto` thay vì Entity thô ở [`DiscountController`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Controller/DiscountController.java#L21).

### 1.4. Đưa OTP Verification sang Redis
- Thay `ConcurrentHashMap` trong [`VerificationCodeService.java`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Service/VerificationCodeService.java) và [`VerifiedEmailService.java`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Service/VerifiedEmailService.java) bằng `StringRedisTemplate` (TTL 5 phút cho OTP, 15 phút cho email đã verify), kết nối trực tiếp tới container `redis` trong `docker-compose.yml`.
- Xóa toàn bộ log in plaintext mã OTP, giới hạn tối đa 5 lần nhập sai và cooldown 60s giữa các lần gửi.

---

## Phase 2: Sửa Triệt để Nghiệp vụ Bán vé, Concurrency, PayOS & QR

### 2.1. Chống Overselling Vé & Voucher
- Trong [`TicketRepository`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Repository/TicketRepository.java) & [`DiscountRepository`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Repository/DiscountRepository.java):
  - Dùng `@Lock(LockModeType.PESSIMISTIC_WRITE)` (luôn **sort `ticketId` tăng dần** trước khi lock để tránh deadlock) hoặc atomic SQL conditional update:
    ```sql
    UPDATE tickets
    SET quantity_sold = quantity_sold + :qty
    WHERE ticket_id = :id AND (quantity_total - quantity_sold) >= :qty
    ```
  - Atomic update tương tự cho `Discount.maxUsage` (`WHERE discount_id = :id AND max_usage > 0`).
- Chuyển toàn bộ các trường tiền tệ (`Ticket.price`, `Order.totalAmount`, `OrderTicket.unitPrice`, `Discount.value`, `Payment.amountPaid`) từ `Double` sang `BigDecimal`.

### 2.2. Tách DB Transaction khỏi PayOS HTTP & Email SMTP
- Trong [`OrderServiceImpl.createOrder`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Service/Impl/OrderServiceImpl.java):
  1. **Transaction 1 (ngắn):** Lock & giữ vé, tạo `Order(pending)` + `OrderTicket`, sinh `payosOrderCode` an toàn (dùng DB sequence / `AtomicLong` + offset, **bỏ** cắt 6 chữ số cuối `System.currentTimeMillis()`).
  2. **Ngoài Transaction:** Gọi `payOS.createPaymentLink(...)`.
  3. **Bù trừ nếu lỗi:** Nếu PayOS fail, mở Transaction 2 chuyển đơn sang `cancelled` và hoàn lại `quantitySold` + `maxUsage`.
- Trong luồng Webhook ([`PaymentController`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Controller/PaymentController.java) → chuyển sang `PaymentService`):
  1. Verify chữ ký PayOS.
  2. **Idempotency check:** Nếu `order.getStatus() == OrderStatus.paid` thì trả `error: 0` ngay (không xử lý lại, không gửi trùng mail).
  3. Cập nhật `order.status = paid`, lưu lịch sử vào [`Payment`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Entity/Payment.java), tạo token QR.
  4. Phát `OrderPaidEvent` và gửi email bằng `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` + `@Async`.

### 2.3. Sửa Hủy đơn, Auto-Cancel Scheduler & Báo cáo
- Xóa endpoint hủy đơn trùng tại [`OrderController.cancelOrder`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Controller/OrderController.java#L184-L235).
- Trong `OrderServiceImpl.cancelOrder`: chỉ cho phép hủy khi `order.getStatus() == OrderStatus.pending` trước khi trừ `quantitySold` (chặn double-cancel gây âm tồn kho).
- Trong `autoCancelPendingOrders`: xử lý từng đơn trong transaction riêng (`TransactionTemplate` / `REQUIRES_NEW`) + `try/catch` riêng từng đơn để 1 đơn lỗi PayOS không rollback cả batch.
- Sửa [`OrderTicketRepository.java`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Repository/OrderTicketRepository.java):
  - Lọc `AND ot.order.status = com.example.event_ticket_system.Enums.OrderStatus.paid` cho `sumQuantityByEvent`, `sumQuantityByTicket`, `findAllByTicketEvent`.
  - Đổi `COUNT(ot)` → `COALESCE(SUM(ot.quantity), 0)` trong `findTicketSoldPerDayByEventId` và sửa `'PAID'` thành `OrderStatus.paid`.

### 2.4. Chuẩn hóa QR Code Chống giả mạo
- Sửa [`QRCodeUtil.java`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Util/QRCodeUtil.java): xóa `shortenQRContent` sai key & `decompressBytes` stub; tạo `checkinToken` ký **HMAC-SHA256** (lưu chuỗi token trong DB, render PNG khi gửi email/tải vé hoặc giữ `byte[]` tương thích DTO hiện tại).

---

## Phase 3: Dọn dẹp Kiến trúc & Thư mục Backend

### 3.1. Xóa thành phần thừa & Thống nhất JPA
- **Xóa [`AccountController.java`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Controller/AccountController.java)** (rỗng, 0 endpoint).
- **Xóa `Mapper/` ([`AccountMapper.java`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Mapper/AccountMapper.java)):** Chuyển 4 query sang [`UserRepository`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Repository/UserRepository.java), gỡ `mybatis-spring-boot-starter` và `mybatis-spring-boot-starter-test` khỏi [`pom.xml`](file:///home/antigravity/dlvanh/event-ticket-system/pom.xml).
- **Xóa [`CheckoutController.java`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Controller/CheckoutController.java) + `resources/static/` + `resources/templates/` + `spring-boot-starter-thymeleaf`**.
- **Thay `@Data` trên JPA Entities** ([`Event`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Entity/Event.java), [`Order`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Entity/Order.java), [`OrderTicket`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Entity/OrderTicket.java), [`Ticket`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Entity/Ticket.java), [`Discount`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Entity/Discount.java), [`Review`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Entity/Review.java)) bằng `@Getter` / `@Setter` + `equals`/`hashCode` theo ID; xóa `isEmpty()` thừa trong [`Gender`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Enums/Gender.java).
- Đổi tên các package PascalCase sang `lowercase`; tách `config/` (PayOS, OpenAPI, Redis, Async) khỏi `security/`.

### 3.2. Global Exception Handler & Dọn Validation
- Thêm `exception/GlobalExceptionHandler` (`@RestControllerAdvice`) + chuẩn hóa `ApiResponse<T>` → xóa toàn bộ `try/catch` lặp trong Controllers; bỏ `spring.main.allow-bean-definition-overriding=true`; thống nhất constructor injection.
- Đổi tên `ValidLogin/` → `validation/`, **bỏ [`ValidLoginPassword`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/ValidLogin/ValidLoginPassword.java)** ở DTO login, và thống nhất rule độ dài mật khẩu giữa `PasswordValidator` và [`OrganizerRequest`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/DTO/request/OrganizerRequest.java#L29).

### 3.3. Xẻ nhỏ `EventServiceImpl` (1.126 dòng) & Dọn Fonts
- Tách [`EventServiceImpl`](file:///home/antigravity/dlvanh/event-ticket-system/src/main/java/com/example/event_ticket_system/Service/Impl/EventServiceImpl.java) thành `EventCommandService` (gắn `@Transactional`), `EventQueryService` + `EventSpecifications` (**sửa bug `root.get("")`** ở dòng 227), `EventReportService`, và `ImageStorageService` (dùng 1 singleton `RestClient`).
- Khử N+1 query ở `getRecommendEvents`, `getListOrders`, `getTicketsByUserId`.
- Trong `resources/fonts/`: chỉ giữ `arial.ttf` và `arialbd.ttf`, xóa 7 file TTF không dùng; đọc font qua `ClassPathResource` (chạy được trong JAR).
- Thêm **SpringDoc OpenAPI** (`springdoc-openapi-starter-webmvc-ui`) để xuất `/v3/api-docs` làm chuẩn cho FE.

---

## Phase 4: Dọn dẹp & Nâng cấp Frontend (`frontend/`) + Đồng bộ Contract

### 4.1. Dọn rác Repository, Chuyển SPA thuần & Cấu hình Giao tiếp BE
- Sửa `frontend/package.json`: xóa self-dependency `"online-ticketing-system-fe": "file:"`, bỏ `express` / `@angular/ssr` / `@angular/platform-server`, cài `@angular-eslint` + `prettier`.
- **Tắt SSR:** Xóa `src/server.ts`, `src/main.server.ts`, `src/app/app.config.server.ts`, `src/app/app.routes.server.ts`, cập nhật `angular.json` sang browser SPA thuần — Nginx trong Docker chỉ việc phục vụ static files + reverse proxy `/api/` sang `backend:8080`.
- Kiểm tra mọi tham chiếu tới `public/dist/` và `public/plugins/` trong `frontend/src/` rồi **xóa toàn bộ ~2.000+ file vendor AdminLTE/Bootstrap/jQuery không sử dụng**.
- Dọn `src/index.html` (xóa FontAwesome 5.15.3 trùng với 6.5.0) và xóa CSS lặp trong `src/styles.css`.
- Tạo `src/environments/environment.ts` (`apiUrl: '/api'`), `environment.development.ts` (`apiUrl: '/api'`), `proxy.conf.json` (cho `ng serve`), và `frontend/nginx.conf` (cho Docker); bỏ sạch `http://localhost:8080` hardcode trong cả 11 services.

### 4.2. Vá Lệch Contract & Logic Frontend
- **`register-organizer`:** Sửa `AuthService.registerOrganizer` và `RegisterOrganizerComponent` gửi `FormData` (`data` JSON Blob + `profilePicture` File) khớp BE; đồng bộ độ dài mật khẩu giữa FE và BE.
- **`AdminService`:**
  - Sửa URL sai `${this.apiUrl}/events/${eventId}/report/excel` (đang thành `/api/users/events/...`) → `/api/events/${eventId}/report/excel` (tương tự cho PDF).
  - Bỏ `{ withCredentials: true }` thừa; xử lý các endpoint admin chết (`/events/{id}/revoke`, `/events/{id}/rejection-reason`, `DELETE /events/{id}`).
- **Components & Interceptors:**
  - Xóa `HttpClientModule` và lệnh gọi `HttpClient` trực tiếp trong component (`export-file-detail.component.ts`...) → chuyển vào Service.
  - Thêm `error.interceptor.ts` xử lý tập trung `401`/`403`/`500`; thay `Observable<any>` bằng interfaces trong `src/app/core/models/` (khớp OpenAPI từ BE).
  - Thống nhất tab routing `<router-outlet>` ở `purchased-ticket.component.ts` khớp `app.routes.ts`.
  - Gỡ `CryptoJS` hardcode key `'eventa-secret-key'` ở `chatbot.service.ts`; xử lý `slider-captcha.component.ts` (bỏ đáp án hardcode client-side & link ảnh Pinterest bên ngoài).

### 4.3. Giảm Lặp Component & Tái cấu trúc Thư mục FE
- Gộp ~9 component slide lặp trong `src/app/user/slides/` thành 1 `EventCarouselComponent` nhận `@Input()`.
- Tổ chức lại theo `core/`, `shared/`, `features/` kèm lazy loading trong `app.routes.ts`.

---

## Phase 5: Kiểm thử Tự động & Verification trong Docker

1. **Backend (`./mvnw verify` với JUnit 5 + Testcontainers MySQL & Redis):**
   - RBAC/Security tests: chặn user thường vào `/api/users/**`, chặn IDOR buyer report, kiểm tra `GET /api/discounts/{code}` không lộ `passwordHash`.
   - Concurrency test: 20 thread đặt đồng thời loại vé còn 5 vé → chính xác 5 đơn thành công, `quantity_sold == 5`, không oversell.
   - PayOS Webhook idempotency test & double-cancel test.
   - Search & PDF export test từ classpath resource (`getEventsByOrganizer` lọc theo `name`, font tiếng Việt).
2. **Frontend (`frontend/`):**
   - `npm ci && npm run lint && npm run build && npm test -- --watch=false --browsers=ChromeHeadless`.
3. **Full-stack E2E Smoke Test (`docker compose up --build`):**
   - Mở trình duyệt vào `http://localhost` (Nginx frontend container gọi API sang `backend:8080`), test trọn vòng đời:
     `Đăng ký + OTP (Redis) -> Login JWT -> Tạo Event + Upload ảnh -> Admin duyệt -> Đặt vé + Discount -> PayOS Checkout/Webhook -> Email QR -> Xuất PDF/Excel`.
