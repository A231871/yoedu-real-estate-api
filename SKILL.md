---
name: spring-boot-backend-standards
description: Coding standards, security rules, error handling conventions, and architecture guidelines for Spring Boot 4.x / Java 21 backend applications (yoedu-real-estate-api).
---

# Spring Boot Backend Coding Standards & Guidelines

This document outlines the architectural patterns, security rules, database guidelines, and coding conventions for the `yoedu-real-estate-api` Spring Boot application.

---

## 1. Core Technology Stack
- **Language**: Java 21+
- **Framework**: Spring Boot 4.0.6 (Spring Framework 7.x)
- **Database**: PostgreSQL 15+ with Flyway migrations (`uuidv7()` default PK generator)
- **Security**: Spring Security + JJWT 0.13.0 (Stateless JWT)
- **Access Token TTL**: 30 minutes
- **Refresh Token TTL**: 7 days
- **Testing**: JUnit 5 + Mockito (`mvn clean test`)

---

## 2. Critical Architecture Rules (DO NOT VIOLATE)

### ⚡ Connection Pool & Hashing Optimization
- **BCrypt Hashing Outside Transactions**: Never execute CPU-heavy password verification (`BCryptPasswordEncoder.matches()`) inside a `@Transactional` boundary. Hashing takes 100–300ms, which would exhaust Hikari connection pool slots under load.
- **Explicit `TransactionTemplate` for `@Modifying` Queries**: When a service method (like `login()`) is unannotated with `@Transactional` for connection pool optimization, use `TransactionTemplate` programmatically to wrap native/JPA `@Modifying` queries.

### 🔒 Optimistic Locking Controls
- **Domain Entities**: Use `@Version` on entities subject to concurrent updates (e.g., `ViewingSchedule`).
- **No `@Version` on `RefreshToken`**: Do **NOT** place `@Version` on `RefreshToken`. Session revocations use direct atomic SQL (`UPDATE ... SET revoked_at = ...`) to prevent lock conflicts during mass token invalidations.

### 🛡️ Soft-Deleted Account Enforcement
- Always filter out archived/soft-deleted users in database lookups (`deleted_at IS NULL`).
- Use repository queries `findByEmailIgnoreCaseAndDeletedAtIsNull(...)` and `findByIdAndDeletedAtIsNull(...)`.
- Explicitly validate `user.getDeletedAt() != null` in auth validators, throwing `BadCredentialsException` or `NotFoundException`.

---

## 3. Security & JWT Conventions

### JWT Principal Handling
- The JWT `subject` contains the user's **UUID string**.
- In controllers and filters, parse the user UUID safely:
  ```java
  UUID userId = UUID.fromString(authentication.getName());
  ```
- For **public endpoints** (e.g., listing view counter), check anonymous status before parsing UUID:
  ```java
  if (authentication != null
          && authentication.isAuthenticated()
          && !(authentication instanceof AnonymousAuthenticationToken)
          && !"anonymousUser".equals(authentication.getPrincipal())) {
      // Safe to parse UUID
  }
  ```

### Refresh Token Rotation (RTR) & Breach Detection
- Save SHA-256 token hashes (`hashToken(refreshToken)`) in the `refresh_tokens` database table.
- **30-Second Concurrency Grace Period**: If a revoked refresh token is submitted within 30 seconds of revocation, allow the request to prevent race condition failures from parallel client SPA calls.
- **Breach Detection**: If a revoked token is reused *after* the 30-second grace period, trigger an immediate emergency mass revocation of **ALL** refresh tokens for that user family (`revokeAllUserTokens`).

### Input & Header Sanitization
- **IP Address Validation**: Validate `X-Forwarded-For` and `X-Real-IP` headers against an IPv4/IPv6 regex pattern. Fall back to `"0.0.0.0"` if malformed to prevent PostgreSQL `inet` data exception crashes.
- **User-Agent Truncation**: Truncate `User-Agent` strings (e.g., to 497 chars) before inserting into session tracking tables.
- **STOMP Role Prefix Guarding**: Prevent `ROLE_ROLE_` double-prefixing in STOMP/WebSocket interceptors:
  ```java
  role -> role.startsWith("ROLE_") ? role : "ROLE_" + role
  ```

### Authorization & IDOR Prevention
- Protect entity-specific endpoints with custom Spring Security evaluators via `@PreAuthorize`:
  ```java
  @GetMapping("/{id}")
  @PreAuthorize("@viewingScheduleSecurity.isParticipant(#id)")
  public ResponseEntity<ApiResponse<ViewingScheduleResponse>> getById(@PathVariable UUID id) { ... }
  ```
- Verify participant rights (client side vs. host/agent side) explicitly in service implementations.

---

## 4. API & Controller Standards

- **Annotations**: `@RestController`, `@RequestMapping("/api/...")`, `@RequiredArgsConstructor`, `@Tag`, `@Operation`.
- **Response Envelope**: Always wrap response payloads in `ApiResponse<T>`:
  ```java
  return ResponseEntity.ok(ApiResponse.success("Success message", data));
  ```
- **Validation**: Annotate DTO request bodies with `@Valid @RequestBody`. Use `@NotBlank`, `@NotNull`, `@Size`, `@DecimalMin`, `@Positive`.
- **Public Endpoints**: Explicitly register public GET catalog endpoints in `SecurityConfig` (`/listing`, `/location/**`, `/amenity`, `/property-type`).

---

## 5. Global Exception Handling

All controllers delegate uncaught exceptions to `GlobalExceptionHandler` (`@RestControllerAdvice`). Standard response mappings:

| Exception Type | HTTP Status | Response Handling |
|---|---|---|
| `NotFoundException` | `404 NOT FOUND` | Custom message |
| `ConflictException` | `409 CONFLICT` | Custom message |
| `BadRequestException` | `400 BAD REQUEST` | Custom message |
| `IllegalArgumentException` | `400 BAD REQUEST` | Custom message |
| `MethodArgumentTypeMismatch` | `400 BAD REQUEST` | Safe parameter mismatch error |
| `UserBannedException` | `403 FORBIDDEN` | Custom banned message |
| `SecurityException` | `403 FORBIDDEN` | Access denied message |
| `AccessDeniedException` | `403 FORBIDDEN` | Access denied message |
| `BadCredentialsException` | `401 UNAUTHORIZED` | Opaque invalid credential message |
| `DataIntegrityViolationException` | `409` or `500` | Constraint-specific message / generic fallback |
| `ObjectOptimisticLockingFailureException` | `409 CONFLICT` | Concurrent modification message |
| `Exception` (Catch-all) | `500 INTERNAL SERVER ERROR` | Opaque message (never leak stack traces) |

---

## 6. Coding Style Checklist
- [ ] Email fields normalized via `.trim().toLowerCase(Locale.ROOT)`.
- [ ] No `System.out.println()` — use `@Slf4j` for logging.
- [ ] JPA entities use `@Getter` and `@Setter` (Lombok); constructors use `@RequiredArgsConstructor`.
- [ ] Repositories extend `JpaRepository<Entity, UUID>`.
- [ ] Timestamps returned in ISO-8601 UTC format.
