# AGENTS.md — Hướng dẫn cho AI Agents & Developers

Tài liệu này cung cấp ngữ cảnh kiến trúc, quy ước mã nguồn và các lệnh kiểm chứng cho monorepo **Event Ticket System (Eventa)**.

## 1. Cấu trúc Monorepo

```
event-ticket-system/
├── backend/                 # Spring Boot 3.4.5 (Java 21, Maven)
│   ├── src/main/java/com/example/event_ticket_system/
│   │   ├── config/          # Async, OpenAPI, PayOS, RestClient
│   │   ├── controller/      # 13 REST controllers (trả về ApiResponse<T>)
│   │   ├── dto/             # request/ & response/ DTOs (không trả Entity trực tiếp)
│   │   ├── entity/          # JPA entities (BigDecimal(15,2) cho tiền tệ)
│   │   ├── enums/           # UserRole, OrderStatus, EventStatus, ApprovalStatus, ...
│   │   ├── event/           # Spring ApplicationEvents (OrderPaidEvent + AFTER_COMMIT listener)
│   │   ├── exception/       # ApiException & GlobalExceptionHandler
│   │   ├── repository/      # Spring Data JPA + PESSIMISTIC_WRITE & atomic UPDATE
│   │   ├── security/        # SecurityConfig, JwtFilter, JwtUtil, CustomUserPrincipal, SecurityUtils
│   │   ├── service/         # Interfaces + EventCommandService, EventQueryService, EventReportService
│   │   └── util/            # QRCodeUtil (HMAC-SHA256), PasswordValidator
│   └── src/test/            # Unit & Integration tests (H2 MySQL mode, 60 tests)
├── frontend/                # Angular 19.2 Pure SPA (không SSR)
│   ├── src/app/
│   │   ├── core/            # models/, interceptors/ (token + error), services/, guards/
│   │   ├── shared/          # EventCarouselComponent & shared UI
│   │   ├── admin/           # AdminLTE 3.2 (nạp CDN theo scope /admin)
│   │   ├── organizer/       # Quản lý sự kiện, báo cáo, thống kê
│   │   └── user/            # Trang chủ, đặt vé, thanh toán PayOS, vé đã mua
│   ├── nginx.conf           # Reverse proxy /api/, /oauth2/, /login/oauth2/ -> backend:8080
│   └── proxy.conf.json      # Dev proxy khi chạy ng serve
├── docker-compose.yml       # MySQL 8 + Redis 7 + Backend + Frontend (Nginx)
├── .env.example             # Mẫu biến môi trường (không commit .env thật)
├── README.md                # Hướng dẫn tổng quan & chạy Docker local
└── IMPLEMENTATION_PLAN.md   # Kế hoạch nâng cấp & tái cấu trúc toàn diện
```

## 2. Lệnh Build & Test chuẩn

### Backend (`backend/`)
- **Compile:** `./mvnw compile` (yêu cầu **JDK 21**)
- **Chạy toàn bộ Unit + Integration Tests (H2):** `./mvnw test`
- **Build & đóng gói Fat JAR:** `./mvnw verify`
- **Lưu ý môi trường test:** Profile `test` ([application-test.yml](backend/src/test/resources/application-test.yml)) dùng H2 với `MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE`, mock `PayOS`, `JavaMailSender`, `StringRedisTemplate` bằng `@MockitoBean`.

### Frontend (`frontend/`)
- **Cài dependencies:** `npm install` (hoặc `npm ci` trong Docker)
- **Dev server (có proxy `/api`):** `npm start`
- **Production build:** `npm run build` (output tại `dist/online-ticketing-system-fe/browser/`)

### Docker Local Stack (tại root)
```bash
cp .env.example .env
docker compose up --build
```

## 3. Quy ước & Ràng buộc Quan trọng

1. **Không dùng Flyway:** Hibernate JPA (`spring.jpa.hibernate.ddl-auto`) quản lý schema trực tiếp.
2. **Bảo mật & Secrets:**
   - Tuyệt đối không hardcode secret/API key/password trong `application.yml` hay code. Luôn dùng `${ENV_VAR}`.
   - Mọi endpoint bảo vệ phải kiểm tra quyền qua `SecurityConfig` + `@PreAuthorize` + kiểm tra ownership (`SecurityUtils.getCurrentUserId()`) để tránh IDOR.
   - Không trả Entity JPA trực tiếp từ Controller; luôn map sang DTO (`ApiResponse<T>`).
3. **Giao dịch & Concurrency:**
   - Đặt vé phải dùng `TicketRepository.findByIdForUpdate` + atomic conditional `UPDATE` (`reserveTickets`) trong `TransactionTemplate` ngắn.
   - External HTTP calls (`PayOS`, `ImgBB`, `Gemini`, `SMTP`) phải nằm **ngoài** DB transaction.
   - Webhook PayOS phải đảm bảo **idempotency**; gửi email QR dùng `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` + `@Async`.
4. **Tiền tệ:** Luôn dùng `BigDecimal` (precision 15, scale 2), không dùng `double`/`float`.
5. **Frontend:**
   - Không hardcode URL backend (`http://localhost:8080` hay ngrok); luôn dùng `environment.apiUrl` (`/api`).
   - Toàn bộ routes trong `app.routes.ts` dùng lazy loading (`loadComponent`).
   - Không tái tạo thư mục `public/dist/` hay `public/plugins/` (AdminLTE đã chuyển sang CDN scoped cho `/admin`).
