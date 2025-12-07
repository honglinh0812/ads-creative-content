# Ads Creative Content Platform

Ads Creative Content is a full-stack web platform that helps marketers generate Facebook ads using Large Language Models (LLMs). The project is composed of a Spring Boot backend, a Vue 3 frontend, and supporting services such as PostgreSQL, Redis, and MinIO. Everything is containerized with Docker so you can run it locally or deploy to a cloud server quickly.

## 1. Architecture Overview

- **Frontend**: Vue 3, Vue Router, Vuex, Ant Design Vue.
- **Backend**: Spring Boot 2.7, Spring Data JPA, Spring Security, Resilience4j, MinIO SDK.
- **Infrastructure**: PostgreSQL (data), Redis (cache), MinIO (media storage), Docker.

## 2. Prerequisites

- Git, Node.js 16+ and npm.
- Java 17, Maven 3.8+.
- Docker Engine and Docker Compose.
- Optional domain name to enable HTTPS (via Let’s Encrypt or an external certificate provider).

### Required Environment Variables

Create a `.env` file in the project root (make a copy from `.env.example`) and keep it private. Below is a minimal template:

```
POSTGRES_DB=postgres-db-name
POSTGRES_USER=postgres-db-user
POSTGRES_PASSWORD=your-postgres-password
REDIS_PASSWORD=your-redis-password
MINIO_ACCESS_KEY=your-minio-access
MINIO_SECRET_KEY=your-minio-secret
MINIO_BUCKET_NAME=fbads-content
APP_JWT_SECRET=64-char-random-string

OPENAI_API_KEY=your-key
AI_GEMINI_API_KEY=your-key
AI_ANTHROPIC_API_KEY=your-key
AI_HUGGINGFACE_API_KEY=your-key

FACEBOOK_APP_ID=facebook-app-id
FACEBOOK_APP_SECRET=facebook-app-secret
FACEBOOK_MARKETING_ACCESS_TOKEN=marketing-token

STORAGE_MINIO_PUBLIC_URL_BASE=https://your-domain/api/images
FACEBOOK_DEFAULT_LINK_URL=https://your-domain
CORS_ALLOWED_ORIGINS=https://your-domain,https://app.your-domain
```

## 3. Deploy with docker-compose

1. **Clone the repository**
   ```bash
   git clone https://github.com/honglinh0812/ads-creative-content.git
   cd ads-creative-content
   ```
2. **Create `.env`** according to section 3.
3. **Start the stack**
   ```bash
   docker-compose up -d
   ```
4. **Verify**
   ```bash
   docker-compose ps
   curl http://localhost:8080/actuator/health
   curl -I http://localhost
   ```
5. **Stop**
   ```bash
   docker-compose down
   ```

## 4. Backend Development Setup

1. Ensure PostgreSQL and Redis are available (local installation or Docker).
2. Optionally override Spring settings via `backend/src/main/resources/application-local.properties`.
3. Install dependencies:
   ```bash
   cd backend
   ./mvnw clean install
   ```
4. Run Spring Boot:
   ```bash
   ./mvnw spring-boot:run
   ```
5. API base URL: `http://localhost:8080` (`/actuator/health` for status).

## 5. Frontend Development Setup

1. Install dependencies:
   ```bash
   cd frontend
   npm install
   ```
2. Configure `frontend/vue.config.js` if you need to proxy API calls to another host.
3. Start dev server:
   ```bash
   npm run serve
   ```
4. UI is available at `http://localhost:8081` (or the port shown in the terminal).