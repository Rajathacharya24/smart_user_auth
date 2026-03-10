# Smart User Auth (Java Backend)

Phase-wise build for secure authentication using Java + Spring Boot.

## Current status
- ✅ Phase 1 complete: project setup, profiles, Flyway migration, Docker Postgres
- ✅ Phase 2 complete: entities + repositories + role seed
- ✅ Phase 3 complete: register/login + BCrypt + JWT tokens + validation
- ✅ Phase 4 complete: JWT security filter chain + auth filter
- ✅ Phase 5 complete: refresh/logout + token rotation
- ✅ Phase 6 complete: protected routes + RBAC (USER/ADMIN)
- ✅ Phase 7 complete: rate limiting + CORS + security headers
- ✅ Phase 8 complete: comprehensive test suite
- ✅ Phase 9 complete: OpenAPI docs + Postman + deployment guide
- 🎉 **PROJECT COMPLETE AND PRODUCTION-READY**

## Prerequisites
- Java 21+
- Maven 3.8+
- PostgreSQL 16+ (or Docker)

## Testing
Run tests with:
```bash
./mvnw test
```

Run integration tests only:
```bash
./mvnw test -Dtest="*IntegrationTest"
```

## Run locally

### Option 1: With Docker
1. Start database:
   ```bash
   docker compose up -d
   ```
2. Start backend:
   ```bash
   ./mvnw spring-boot:run
   ```

### Option 2: Without Docker
1. Install PostgreSQL and create database:
   ```bash
   createdb smart_user_auth
   ```
2. Update `.env` with your PostgreSQL credentials
3. Start backend:
   ```bash
   ./mvnw spring-boot:run
   ```

## API Endpoints

### Auth Endpoints (Public)
- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/login` - Login user
- `POST /api/v1/auth/refresh` - Refresh access token
- `POST /api/v1/auth/logout` - Logout (revoke refresh token)

### User Endpoints (Protected - requires Bearer token)
- `GET /api/v1/users/me` - Get current user profile

### Admin Endpoints (Protected - requires ADMIN role)
- `GET /api/v1/admin/users` - Get all users (admin only)

### Example Register Request
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

### Example Protected Request
```bash
curl -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  http://localhost:8080/api/v1/users/me
```

### Default Admin Account
- Email: `admin@smartauth.com`
- Password: `AdminPass123!`

## API Documentation

### Swagger UI (OpenAPI)
Once the application is running, access interactive API docs at:
- Local: http://localhost:8080/swagger-ui.html
- API Spec: http://localhost:8080/v3/api-docs

### Postman Collection
Import the collection from:
- [docs/Smart_User_Auth_API.postman_collection.json](docs/Smart_User_Auth_API.postman_collection.json)

## Documentation

- [Product Requirements Document](docs/PRD_secure_user_auth.md)
- [Implementation Checklist](docs/IMPLEMENTATION_CHECKLIST.md)
- [Deployment Guide](docs/DEPLOYMENT.md)
- [Incident Response Runbook](docs/RUNBOOK.md)
- [Workflow Diagram](docs/auth-workflow.tldr)

### Example Login Request
```json
{
  "email": "john@example.com",
  "password": "SecurePass123!"
}
```

## Environment
Copy `.env.example` to `.env` and adjust values as needed.

Default profile: `dev`
