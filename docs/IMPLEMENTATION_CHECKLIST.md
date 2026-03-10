# Implementation Checklist (Execution Order)

## Phase 0 — Planning
- [x] Confirm final stack: Spring Boot + PostgreSQL + JWT
- [x] Finalize token strategy (cookie vs header for refresh token) — use httpOnly secure cookie for refresh token (web), Authorization header fallback for API clients
- [x] Finalize password policy — min 8 chars, upper/lower/digit/special

## Phase 1 — Project Setup
- [x] Initialize Spring Boot project (`web`, `security`, `data-jpa`, `validation`)
- [x] Configure environment profiles (`dev`, `prod`, `test`)
- [x] Add Flyway and create initial migration
- [x] Set up Docker Compose for PostgreSQL

## Phase 2 — Database & Domain
- [x] Create entities: `User`, `Role`, `RefreshToken`
- [x] Create repositories
- [x] Seed default roles (`ROLE_USER`, `ROLE_ADMIN`)
- [x] Add constraints and indexes (email unique, token lookup)

## Phase 3 — Registration & Login
- [x] Implement register endpoint with input validation
- [x] Hash password with BCrypt/Argon2
- [x] Implement login endpoint with credential validation
- [x] Return access + refresh tokens on success

## Phase 4 — JWT & Security Filter Chain
- [x] Implement JWT utility (create, validate, parse claims)
- [x] Configure `SecurityFilterChain`
- [x] Add auth filter to read bearer token
- [x] Implement custom auth entry point (401) and access denied handler (403)

## Phase 5 — Refresh/Logout Session Controls
- [x] Store hashed refresh tokens in DB
- [x] Implement refresh endpoint with token rotation
- [x] Implement logout endpoint with token revocation
- [x] Add optional token/device tracking

## Phase 6 — Protected APIs & RBAC
- [x] Add `/users/me` protected endpoint
- [x] Add admin-only endpoint(s)
- [x] Enforce role checks with annotations and route rules

## Phase 7 — Security Hardening
- [x] Add rate limiter on `/auth/login` and `/auth/register`
- [x] Add account lockout policy after repeated failures
- [x] Configure CORS trusted origins
- [x] Add secure headers (HSTS, CSP basics)

## Phase 8 — Testing
- [x] Unit tests for auth services
- [x] Integration tests for end-to-end auth flow
- [x] Negative tests (expired token, revoked refresh, wrong role)
- [x] Load sanity checks for auth endpoints

## Phase 9 — Documentation & Handover
- [x] Generate OpenAPI docs
- [x] Export Postman collection
- [x] Add deployment instructions
- [x] Add runbook for incident handling/auth token issues

## Definition of Done
- [x] All acceptance criteria from PRD met
- [x] CI passing
- [x] No critical security findings in basic scan
- [x] Ready for staging deployment
