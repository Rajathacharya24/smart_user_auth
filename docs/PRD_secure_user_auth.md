# Product Requirements Document (PRD)
## Project: Secure User Authentication System

## 1) Objective
Build a production-ready authentication and authorization system where users can:
- Sign up
- Log in securely
- Access protected routes/resources only after authentication
- Be controlled by role-based access (`USER`, `ADMIN`)

Backend must be Java-based.

## 2) Problem Statement
Most early-stage apps ship with weak auth patterns (plain passwords, no session control, no refresh strategy, weak route protection). This project solves that by delivering a secure and scalable auth foundation.

## 3) Scope
### In Scope
- User registration
- Secure login/logout
- Password hashing
- JWT access + refresh token flow
- Protected APIs
- RBAC middleware (method and endpoint level)
- Basic user profile endpoint
- Error handling and audit logging

### Out of Scope (Phase 1)
- OAuth social login (Google/GitHub)
- MFA/2FA
- Enterprise SSO (SAML/OIDC)

## 4) Users and Roles
- `USER`: normal app access
- `ADMIN`: elevated access for admin endpoints

## 5) Functional Requirements
### FR-1 Registration
- User can register with name, email, password.
- Email must be unique.
- Password must meet policy:
  - Minimum 8 chars
  - Uppercase + lowercase + digit + special char
- Password stored only as hash (BCrypt/Argon2).
- Default role = `USER`.

### FR-2 Login
- User logs in via email + password.
- On success, return:
  - Access token (short TTL, e.g., 15 min)
  - Refresh token (long TTL, e.g., 7 days)
- On failure, return generic error (avoid user enumeration).

### FR-3 Token Refresh
- Valid refresh token can generate a new access token.
- Rotation recommended (invalidate old refresh token once used).

### FR-4 Logout
- Logout invalidates current refresh token.
- Optional: blacklist access token until expiry.

### FR-5 Protected Routes
- Requests to protected resources require valid access token.
- Unauthorized requests return 401.

### FR-6 RBAC
- Admin endpoints require `ADMIN` role.
- Non-admin users get 403 for admin resources.

### FR-7 Account Security
- Optional lockout after repeated failed logins.
- Optional email verification flow.

### FR-8 Audit Logging
- Log auth events: signup, login success/failure, refresh, logout, role changes.

## 6) Non-Functional Requirements
- Security: OWASP-aligned implementation.
- Performance: Auth APIs should respond in < 300 ms under normal load.
- Availability: Backend should be stateless (except token persistence).
- Observability: Structured logs + error tracking.
- Maintainability: Layered architecture + test coverage.

## 7) Suggested Tech Stack (Java Backend)
- Java 21
- Spring Boot 3.x
- Spring Security 6
- Spring Data JPA
- PostgreSQL
- Flyway (migrations)
- JWT library (Nimbus or jjwt)
- Redis (optional token blacklist/rate limiting)
- Testcontainers + JUnit 5 + Mockito

## 8) Data Model (Phase 1)
### `users`
- `id` (UUID)
- `name`
- `email` (unique)
- `password_hash`
- `is_enabled`
- `created_at`, `updated_at`

### `roles`
- `id`
- `name` (`ROLE_USER`, `ROLE_ADMIN`)

### `user_roles`
- `user_id`
- `role_id`

### `refresh_tokens`
- `id` (UUID)
- `user_id`
- `token_hash`
- `issued_at`
- `expires_at`
- `revoked_at`
- `device_info` (optional)

## 9) API Contract (v1)
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /api/v1/users/me` (protected)
- `GET /api/v1/admin/users` (admin only)

## 10) Security Requirements
- Hash passwords with BCrypt (cost factor ~12) or Argon2.
- Never store plain refresh token; store hashed refresh token.
- Use HTTPS only in production.
- Use secure, httpOnly cookies for refresh token when possible.
- Add rate limiting for login and register endpoints.
- CORS whitelist only trusted frontend origins.
- Input validation on all payloads.

## 11) Acceptance Criteria
1. User can register and cannot register with duplicate email.
2. User can log in and receives valid tokens.
3. Protected route rejects unauthenticated calls (401).
4. Admin route blocks non-admin users (403).
5. Refresh endpoint issues a new access token with valid refresh token.
6. Logout revokes refresh token and prevents future refresh.
7. Passwords are hashed and never returned in API responses.
8. Automated tests pass for core auth flows.

## 12) Milestones
### M1: Foundation (Day 1-2)
- Project setup, database, migrations, base entities

### M2: Core Auth (Day 3-5)
- Register/login, JWT generation, security filters

### M3: Authorization + Session Controls (Day 6-7)
- RBAC, refresh flow, logout revoke

### M4: Hardening + Testing (Day 8-10)
- Rate limit, lockout, integration tests, docs

## 13) Risks and Mitigations
- **Token theft** → short access TTL + refresh rotation + secure storage.
- **Brute force** → rate limit + lockout.
- **Misconfigured CORS** → explicit environment-based whitelist.
- **Schema drift** → Flyway migrations + CI checks.

## 14) Deliverables
- Java Spring Boot auth service
- Migration scripts
- API docs (OpenAPI/Swagger)
- Postman collection
- Test suite (unit + integration)
- Deployment-ready env configs
