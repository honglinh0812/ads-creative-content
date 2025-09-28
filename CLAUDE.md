# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What you need to follow

* Always read entire files. Otherwise, you don’t know what you don’t know, and will end up making mistakes, duplicating code that already exists, or misunderstanding the architecture.  
* Commit early and often. When working on large tasks, your task could be broken down into multiple logical milestones. After a certain milestone is completed and confirmed to be ok by the user, you should commit it. If you do not, if something goes wrong in further steps, we would need to end up throwing away all the code, which is expensive and time consuming.  
* Your internal knowledgebase of libraries might not be up to date. When working with any external library, unless you are 100% sure that the library has a super stable interface, you will look up the latest syntax and usage via either Perplexity (first preference) or web search (less preferred, only use if Perplexity is not available)  
* Do not say things like: “x library isn’t working so I will skip it”. Generally, it isn’t working because you are using the incorrect syntax or patterns. This applies doubly when the user has explicitly asked you to use a specific library, if the user wanted to use another library they wouldn’t have asked you to use a specific one in the first place.  
* Always run linting after making major changes. Otherwise, you won’t know if you’ve corrupted a file or made syntax errors, or are using the wrong methods, or using methods in the wrong way.   
* Please organise code into separate files wherever appropriate, and follow general coding best practices about variable naming, modularity, function complexity, file sizes, commenting, etc.  
* Code is read more often than it is written, make sure your code is always optimised for readability  
* Unless explicitly asked otherwise, the user never wants you to do a “dummy” implementation of any given task. Never do an implementation where you tell the user: “This is how it *would* look like”. Just implement the thing.  
* Whenever you are starting a new task, it is of utmost importance that you have clarity about the task. You should ask the user follow up questions if you do not, rather than making incorrect assumptions.  
* Do not carry out large refactors unless explicitly instructed to do so.  
* When starting on a new task, you should first understand the current architecture, identify the files you will need to modify, and come up with a Plan. In the Plan, you will think through architectural aspects related to the changes you will be making, consider edge cases, and identify the best approach for the given task. Get your Plan approved by the user before writing a single line of code.   
* If you are running into repeated issues with a given task, figure out the root cause instead of throwing random things at the wall and seeing what sticks, or throwing in the towel by saying “I’ll just use another library / do a dummy implementation”.   
* You are an incredibly talented and experienced polyglot with decades of experience in diverse areas such as software architecture, system design, development, UI & UX, copywriting, and more.  
* When doing UI & UX work, make sure your designs are both aesthetically pleasing, easy to use, and follow UI / UX best practices. You pay attention to interaction patterns, micro-interactions, and are proactive about creating smooth, engaging user interfaces that delight users.   
* When you receive a task that is very large in scope or too vague, you will first try to break it down into smaller subtasks. If that feels difficult or still leaves you with too many open questions, push back to the user and ask them to consider breaking down the task for you, or guide them through that process. This is important because the larger the task, the more likely it is that things go wrong, wasting time and energy for everyone involved.

## Project Overview

This is a **Facebook Ads Creative Content Generation** web application that uses AI to automatically generate ad content including text and images. The project consists of a Spring Boot backend with Vue.js frontend, designed to help users create and optimize Facebook advertising campaigns.

## Technology Stack

### Backend (Spring Boot 2.7.8)
- **Language**: Java 17
- **Framework**: Spring Boot with Spring Security, Spring Data JPA
- **Database**: PostgreSQL with Flyway migrations
- **Cache**: Redis 
- **AI Integration**: Multiple providers (OpenAI, Gemini, Anthropic, HuggingFace, Stable Diffusion, Fal.ai)
- **Authentication**: OAuth2 (Facebook) + JWT
- **Resilience**: Resilience4j for circuit breakers, retries, timeouts
- **API Documentation**: SpringDoc OpenAPI
- **Build Tool**: Maven

### Frontend (Vue.js 3)
- **Framework**: Vue 3 with Composition API
- **UI Library**: Ant Design Vue 4.2.6
- **State Management**: Vuex 4
- **Routing**: Vue Router 4
- **HTTP Client**: Axios
- **Charts**: Chart.js with vue-chartjs
- **Styling**: SCSS + Tailwind CSS
- **Build Tool**: Vue CLI 5

## Common Development Commands

### Backend Commands
```bash
# Build and run (from backend/ directory)
mvn clean package -DskipTests
mvn spring-boot:run

# Run with tests
mvn clean test
mvn clean verify

# Build production JAR
mvn clean package

# Database migration
mvn flyway:migrate

# Generate effective POM for dependency analysis
mvn help:effective-pom
```

### Docker Development
```bash
# Start all services including MinIO
docker-compose up -d

# Start only backend services (PostgreSQL, Redis, MinIO)
docker-compose up -d postgres redis minio

# View logs
docker-compose logs -f backend

# Stop all services
docker-compose down

# MinIO Console Access
# URL: http://localhost:9001
# Default credentials: minioadmin/minioadmin
```

### Storage Configuration
```bash
# Local Storage (default)
export STORAGE_TYPE=local
export STORAGE_LOCAL_BASE_PATH=uploads

# MinIO Storage (recommended for production)
export STORAGE_TYPE=minio
export STORAGE_MINIO_ENDPOINT=http://localhost:9000
export STORAGE_MINIO_ACCESS_KEY=minioadmin
export STORAGE_MINIO_SECRET_KEY=minioadmin
export STORAGE_MINIO_BUCKET_NAME=ads-content
```

### Frontend Commands
```bash
# Development server (from frontend/ directory)
npm run serve

# Build for production
npm run build

# Linting
npm run lint
npm run lint:css

# CSS linting with auto-fix
npm run lint:css:fix

# Run tests
npm run test
```

### Deployment
```bash
# Deploy backend to server
./deployment/deploy-backend.sh

# Deploy frontend to server
./deployment/deploy-frontend.sh

# Initial server setup
./deployment/backend-setup.sh
```

## Architecture

### Backend Architecture
- **Controllers**: Handle HTTP requests (`/api/ads`, `/api/auth`, `/api/campaigns`, etc.)
- **Services**: Business logic layer with AI content generation, campaign management
- **AI Providers**: Pluggable AI provider system supporting multiple services
- **Models**: JPA entities (Ad, Campaign, User, AdContent)
- **Security**: JWT + OAuth2 authentication with Facebook integration
- **Configuration**: Extensive configuration for database, Redis, AI providers, resilience patterns

### Frontend Architecture
- **Views**: Main pages (Dashboard, AdCreate, Campaign management, Analytics)
- **Components**: Reusable UI components with mobile-responsive design
- **Store**: Vuex modules for state management (auth, ads, campaigns, dashboard)
- **Services**: API integration layer with axios interceptors
- **Router**: Protected routes with authentication guards

### AI Integration
The application supports multiple AI providers with fallback mechanisms:
- **Text Generation**: OpenAI GPT, Gemini, Anthropic Claude, HuggingFace
- **Image Generation**: OpenAI DALL-E, Stable Diffusion, Fal.ai
- **Resilience**: Circuit breakers, retries, and timeouts configured per provider
- **Validation**: Content validation service to ensure generated content meets Facebook ad guidelines

### Storage Architecture
The application uses a pluggable storage abstraction layer:
- **Local Storage**: Default file system storage for development
- **MinIO Storage**: S3-compatible object storage for production
- **Storage Service**: Unified interface supporting multiple backends
- **Configuration**: Environment-based storage type selection (`STORAGE_TYPE=local|minio`)
- **Features**: Automatic bucket creation, public URL generation, presigned URLs, file metadata

## Key Configuration Files

### Backend Configuration
- `backend/src/main/resources/application.properties` - Main configuration
- `backend/pom.xml` - Maven dependencies and build configuration
- `backend/src/main/resources/db/migration/` - Flyway database migrations

### Frontend Configuration  
- `frontend/package.json` - Dependencies and scripts
- `frontend/vue.config.js` - Vue CLI configuration
- `frontend/tailwind.config.js` - Tailwind CSS configuration
- `frontend/src/router/index.js` - Route definitions

## Database Schema
The application uses PostgreSQL with Flyway for version control:
- **Users**: User authentication and profile data
- **Campaigns**: Facebook campaign configurations  
- **Ads**: Generated ad content and metadata
- **Ad_Content**: Versioned ad content storage

## Development Guidelines

### Environment Setup
1. **Backend**: Requires Java 17, Maven, PostgreSQL, Redis
2. **Frontend**: Requires Node.js 16+, npm
3. **AI Providers**: Configure API keys in `application.properties`
4. **Database**: Run Flyway migrations on startup

### Testing Strategy
- Backend: JUnit tests with Spring Boot Test
- Frontend: Vue Test Utils with Jest (configured but tests need implementation)
- Integration: API testing through controller tests

### Security Considerations
- API keys stored in configuration (should be moved to environment variables for production)
- CORS configured for specific origins
- JWT token-based authentication
- OAuth2 integration with Facebook

### Performance Optimizations
- HikariCP connection pooling configured
- Redis caching for AI provider health and content
- Database indexing and query optimization
- Circuit breakers prevent cascade failures
- Async processing with `@EnableAsync`

## Resource Requirements

### Technical Skills Needed:
- **DevOps**: Docker Compose, CI/CD pipelines, Linux system administration
- **Backend**: Spring Boot, testing frameworks, cloud SDKs  
- **Frontend**: Vue.js testing, E2E testing tools
- **Monitoring**: Sentry integration, structured logging, Spring Boot Actuator

### Infrastructure Decision: Docker Compose
**Current Recommendation: Docker Compose**
- **Rationale**: Single VM deployment, monolithic architecture, small team
- **Benefits**: Lower complexity, faster implementation, cost-effective

### Risk Mitigation:
- Use feature flags for gradual rollouts
- Comprehensive testing in staging environment before production

## System Architecture Improvements

### Current Priority: UI/UX Overhaul (Critical)

The frontend currently has significant issues that need immediate attention:

**Critical Problems Identified:**
1. **AI-Generated Aesthetic**: Interface looks artificial with excessive gradients, perfect spacing, and generic design patterns
2. **Mobile Responsiveness**: Incomplete mobile implementation across pages
3. **Cross-Page Inconsistency**: Different UI patterns, navigation styles, and component usage between pages
4. **Generic Corporate Look**: Needs student developer personality and creativity

**UI Improvement Requirements:**
- **Remove AI-generated look**: Replace gradients, excessive cards, perfect mathematical spacing with authentic design
- **Complete mobile responsiveness**: Ensure all pages work seamlessly on mobile devices
- **Standardize component usage**: Consistent implementation of Ant Design Vue across all pages
- **Add creative character**: Design should feel like it was built by a talented student developer, not AI

**Technical Stack for UI Work:**
- Vue 3 with Composition API
- Ant Design Vue 4.2.6 (primary UI library)
- Tailwind CSS (well-configured color system and utilities)
- SCSS for custom styling
- Mobile-first responsive design approach

**Implementation Phases:**
1. **Design System Foundation** - Remove AI aesthetics, create authentic design language
2. **Mobile-First Responsive** - Complete mobile implementation for all pages
3. **Creative Layout Implementation** - Add student developer personality and creativity
4. **Consistency & Polish** - Ensure cohesive experience across all pages
5. **Character Addition** - Personal touches that show developer creativity

See detailed analysis and plan: **[docs/ui-improvement.md](./docs/ui-improvement.md)**

### Architecture Notes
- All 7 microservices are fully implemented and containerized
- API Gateway routes requests to appropriate microservices
- Each service has its own PostgreSQL database for data separation
- Health checks and monitoring are implemented across all services

## Production Deployment
- Backend: Deployed as systemd service with JAR file
- Frontend: Static files served by Nginx
- Database: PostgreSQL with regular backups
- SSL: Certbot for SSL certificate management
- Monitoring: Basic actuator endpoints enabled