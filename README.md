# Event Ticket System (Eventa) — Monorepo

Full-stack event ticketing platform combining the **Java 21 / Spring Boot 3.4** backend (`backend/`) and **Angular 19** SPA frontend (`frontend/`).

## Structure

```text
event-ticket-system/
├── backend/            # Java 21 + Spring Boot 3.4.5 REST API (JPA, MySQL, Redis, PayOS)
├── frontend/           # Angular 19 SPA + Nginx reverse proxy for Docker
├── docker-compose.yml  # Local full-stack orchestration (MySQL + Redis + Backend + Frontend)
└── .env.example        # Required environment variables template
```

## Quick Start with Docker

1. Copy environment variables and fill in your credentials (JWT secret, SMTP, PayOS, ImgBB, Gemini):
   ```bash
   cp .env.example .env
   ```
2. Start the full stack (MySQL, Redis, Backend, Frontend):
   ```bash
   docker compose up --build
   ```
3. Access the application:
   - Frontend: `http://localhost:4200` (Nginx proxies `/api/*` directly to `backend:8080`)
   - Backend API: `http://localhost:8080`
   - Swagger UI / OpenAPI: `http://localhost:8080/swagger-ui.html`

## Local Development (without containerizing apps)

1. Start only MySQL & Redis:
   ```bash
   docker compose up -d mysql redis
   ```
2. Run Backend (`Java 21`):
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```
3. Run Frontend (`Node 22+`):
   ```bash
   cd frontend
   npm ci
   npm start
   ```
   The Angular dev server uses `proxy.conf.json` to forward `/api/*` requests to `http://localhost:8080`.
