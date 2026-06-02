# Lolita Laundry

Web-based SaaS for **Lolita Laundry**, a laundry vendor in Bandung, Indonesia. Replaces Google Form + AppSheet + Google Sheets with a unified system for managing orders, tracking deliveries, and generating invoices across multiple client businesses.

---

## Tech Stack

| Layer | Choice |
|---|---|
| Backend | Spring Boot 4.0.x · Spring Modulith · Hexagonal Architecture |
| JDK | Eclipse Temurin 25 |
| Build | Gradle Kotlin DSL |
| Auth | Auth0 |
| Database | PostgreSQL (Neon.tech) |
| File storage | Cloudflare R2 |
| Frontend | React 19 + Vite → Vercel |
| Hosting | Oracle Cloud Always Free ARM |

---

## Documentation

Full documentation lives in the **[Wiki](../../wiki)**:

- [Architecture Overview](../../wiki/Architecture-Overview)
- [Tech Stack](../../wiki/Tech-Stack)
- [Domain Model](../../wiki/Domain-Model)
- [Business Logic](../../wiki/Business-Logic)
- [API Design](../../wiki/API-Design)
- [Development Setup](../../wiki/Development-Setup)
- [Deployment](../../wiki/Deployment)
- [Decisions Log](../../wiki/Decisions-Log)

---

## Quick Start (Development)

**Prerequisites:** Docker Desktop, Eclipse Temurin JDK 25, Node.js 22+

```bash
# 1. Start backing services (PostgreSQL + MinIO)
docker compose up -d

# 2. Run backend
cd backend && ./gradlew bootRun

# 3. Run frontend
cd frontend && npm install && npm run dev
```

Backend → `http://localhost:8080`
Frontend → `http://localhost:5173`
MinIO console → `http://localhost:9001` (minioadmin / minioadmin)

See [Development Setup](../../wiki/Development-Setup) for Auth0 configuration and full setup guide.

---

## License

MIT © Salman Manoe
