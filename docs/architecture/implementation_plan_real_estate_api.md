# Real Estate Platform Implementation Plan

This document outlines the detailed roadmap for the next phase of the Real Estate & Rental Platform backend. The database schema (V1-V9) and the base Spring Boot setup are complete. This plan divides the remaining work into 3 isolated development lanes to allow parallel work without merge conflicts, provides detailed task tickets, and enforces architectural alignment with the standard team practices.

## Real Estate Platform - Execution Timeline Tree

```text
Real Estate Platform - Chronological Execution Tree
(Total Estimated Time: 3 Sprints / 6 Weeks)

├── Sprint 1 (Weeks 1-2): Foundation & High-Risk Core
│   │
│   ├── Week 1: Bootstrapping & Read Models
│   │   ├── [Phase 0] Run Baseline Migrations (V1-V9) & PostGIS config (Team/Lead)
│   │   ├── [Phase 0] Setup Spring Modulith Interfaces & Global Configs (Team/Lead)
│   │   ├── [Dev 1] Task 1.1: Lookup Data APIs (Provinces, Amenities)
│   │   └── [Dev 3] Task 3.1a: Basic Chat Persistence & User Profiles
│   │
│   └── Week 2: Heavy Tech & Core Algorithms
│       ├── [Dev 1] Task 1.2: PostGIS Search & PostgreSQL Full-Text Search (FTS)
│       ├── [Dev 2] Task 2.1: Viewing Schedules (DST logic & Postgres Exclusion Constraints)
│       └── [Dev 3] Task 3.1b: STOMP WebSockets Authentication & Connection Management
│
├── Sprint 2 (Weeks 3-4): State Mutations, Async Processes & Media
│   │
│   ├── Week 3: CRUD & Data Buffering
│   │   ├── [Dev 1] Task 1.3a: Listing Management API (Drafts, Validations)
│   │   ├── [Dev 2] Task 2.2: Favorites & Redis View Count Buffering
│   │   └── [Dev 3] Task 3.2: Firebase FCM/APNS Notification UPSERT Logic
│   │
│   └── Week 4: Background Workers & Moderation
│       ├── [Dev 1] Task 1.3b: S3 Presigned URLs & Async Image Compression Worker
│       ├── [Dev 2] Task 2.3: Validated Reviews & Report Event Handoff
│       └── [Dev 3] Task 3.3: Admin Dashboard (Moderation Queues & Envers Auditing)
│
└── Sprint 3 (Weeks 5-6): Event Choreography, Integration & Hardening
    │
    ├── Week 5: Cross-Module Integration (Connecting the Modulith)
    │   ├── [Dev 1 & Dev 3] Wire Admin Suspension Events to Listing Status
    │   ├── [Dev 1 & Dev 2] Wire Suspended Listings to View Schedule Auto-Cancellations
    │   └── [Dev 2 & Dev 3] Wire Booking/Chat Events to Push Notifications
    │
    └── Week 6: QA & Deployment Prep
        ├── [Team] Write cross-boundary `@DataJpaTest` Integration Tests (Testcontainers)
        ├── [Team] Performance validation (Verify JEP 519 memory, N+1 query checks)
        └── [Team] Staging Deployment & Stakeholder Sign-off

```

## Sprint Breakdown
*   **Sprint 1 (Weeks 1-2):** Phase 0 baseline is completed in the first 3 days. Devs immediately transition into their hardest technical challenges: Dev 1 tackles PostGIS/FTS, Dev 2 tackles Timezones & Exclusion Constraints, and Dev 3 tackles WebSocket Handshakes.
*   **Sprint 2 (Weeks 3-4):** The focus shifts to state management and async processes. Dev 1 builds the S3 external compression, Dev 2 implements the Redis buffer and reviews, and Dev 3 builds the Push Notifications and Admin Moderation polling.
*   **Sprint 3 (Weeks 5-6):** Buffer sprint for Integration Testing (Testcontainers), cross-module edge cases, QA bug fixes, and Final Staging Deployment.

## User Review Required

> [!IMPORTANT]
> Please review the Domain-Driven Delegation strategy below to ensure the distribution of work among the three developers fits your team's skills and expectations. Once approved, developers can immediately pick up their tickets.

## 1. Phase 0: Baseline Contracts & Isolation
Before any feature work begins, the team must execute Phase 0:
- **Baseline Migrations:** The initial V1-V9 schema must be merged to `main` to enforce a linear, chronological migration history. **CRITICAL:** Include `CREATE EXTENSION IF NOT EXISTS btree_gist;` so that PostgreSQL GiST indexes support the `=` operator for scalar types like UUIDs or BIGINTs.
- **Interface Segregation & Public API Contracts:** Do NOT centralize all interfaces into a shared `core` or `api` package (the "God module" anti-pattern). **CRITICAL:** Each module (Dev 1, 2, and 3) must expose its *own* inbound API (Interfaces/DTOs) that other modules can depend on. Devs will use standard Mockito for unit tests and simple stub classes for local boot during Phase 0. Do NOT over-complicate with `@Profile("dev")` beans.

## 2. Domain-Driven Delegation (Parallel Development Lanes)

To prevent Git merge conflicts and ensure clear boundaries, the backend features are divided into 3 distinct domains.

> **Note on User Identity & Authentication:** The User domain (login, registration, security context) is already established in the base Spring Boot setup (e.g., `AuthController`). All 3 lanes will interact with users by utilizing Spring Security's `Principal` or `@AuthenticationPrincipal` in the Controller layer to extract the current user's ID/Role.

### Dev 1: Core Listings & Location Domain
**Focus:** Public search, listing management (CRUD), and lookup data.
**Tables Owned/Interacted:** `listings`, `listing_images`, `listing_amenities`, `property_types`, `amenities`, `provinces`, `districts`, `wards`

### Dev 2: Booking & Interactions Domain
**Focus:** Handling viewing schedules, user favorites, listing views, and reviews. Dev 2 handles the frontend API for reporting, but Dev 3 physically owns the data persistence.
**Tables Owned/Interacted:** `viewing_schedules`, `favorites`, `listing_views`, `reviews`

### Dev 3: Messaging, Notifications & Admin Domain
**Focus:** In-app messaging between users, system notifications, and admin moderation tools.
**Tables Owned/Interacted:** `conversations`, `messages`, `notifications`, `push_tokens`, Envers `_aud` tables, `reports` (Strict Owner), `users`

---

## 2. Detailed Task Tickets

### Lane 1: Dev 1 (Listings & Location)

#### Task 1.1: Lookup Data APIs
- **Tables:** `provinces`, `districts`, `wards`, `property_types`, `amenities`
- **Required Layers:** `LocationController`, `LookupController`, `LocationService`, `LookupService`, Repositories, DTOs.
- **Acceptance Criteria:**
  - GET endpoints to retrieve provinces, districts by province, and wards by district.
  - GET endpoints to retrieve active property types and amenities.
  - Data must be read-only (cached where applicable).

#### Task 1.2: Public Listing Search API
- **Tables:** `listings` (Core entity), `listing_amenities`
- **Required Layers:** `ListingSearchController`, `ListingSearchService`, `ListingRepository`, MapStruct Mappers.
- **Acceptance Criteria:**
  - **CRITICAL POSTGIS WARNING:** For the `location` column, you MUST use the `geography(Point, 4326)` type, NOT `geometry(Point, 4326)`. `ST_DWithin` on `geometry` calculates in degrees (scanning the planet), whereas `geography` correctly calculates in meters.
  - **CRITICAL:** Split into two distinct endpoints due to conflicting pagination patterns. Only `APPROVED` status listings can be searchable.
  - GET `/api/public/listings` (Feed): **CRITICAL PAGINATION WARNING:** Keyset pagination is strictly forbidden when sorting by highly mutable fields (like `price` or `views`). Even with a tie-breaker, items changing price mid-session will jump ranges and cause users to miss items entirely. For mutable sorting, use **Offset Pagination** and enforce a strict hard limit (e.g., max 10-20 pages) to prevent deep-pagination bottlenecks. Keyset pagination should only be used on immutable/append-only sort keys (e.g., `created_at DESC, id DESC`). **SEARCH UX CRITICAL:** If "Contact for Price" listings have a `NULL` price, sorting by `price DESC` will default to `NULLS FIRST` in Postgres, flooding the top of search results with unpriced listings. Dev 1 MUST explicitly mandate `ORDER BY price DESC NULLS LAST` in Spring Data/Native SQL queries.
  - GET `/api/public/listings/search` (FTS): Must utilize PostgreSQL's Full-Text Search (`tsvector` + GIN). **CRITICAL PERFORMANCE:** Computing `to_tsvector` on read is a massive CPU bottleneck. Dev 1's Flyway schema MUST define a **Stored Generated Column**. **CRITICAL NULL BUG:** You MUST use COALESCE, because `NULL` concatenated with a string yields `NULL`, wiping out search results if a description is blank. Define it as: `search_vector tsvector GENERATED ALWAYS AS (to_tsvector('english', coalesce(title, '') || ' ' || coalesce(description, ''))) STORED`. The API queries this indexed column. Limit the raw SQL query explicitly (e.g., max 100 results) instead of deep pagination. Long-term, FTS should be offloaded to Elasticsearch/Typesense.
  - **Backend:** Java 25 (LTS), Spring Boot 4.1.x, Spring Modulith 2.1+, Spring Security (JWT), Spring Data JPA, Hibernate.
  - **2026 Optimization:** Enable **Compact Object Headers (JEP 519)** in the JVM args (`-XX:+UseCompactObjectHeaders`) to slash the memory footprint of millions of loaded listings by up to 20%. **ASYNC CONTEXT WARNING:** Java 25 `ScopedValue` is designed exclusively for Structured Concurrency. It cannot be used to pass the Spring Security Principal to detached async Outbox listeners because the parent web thread exits before the listener fires. Context must be explicitly serialized into the event payload (e.g., store `triggered_by_user_id` inside the Outbox event table).
  - **Database:** PostgreSQL 16 (with PostGIS extension), Redis (caching, real-time message brokering, scheduling). Returns summarized listing DTOs including the `is_primary` image.

#### Task 1.3: Listing Management (Host/Agent)
- **Tables:** `listings`, `listing_images`, `listing_amenities`
- **Required Layers:** `ListingManagementController`, `ListingManagementService`, Repositories, Request/Response DTOs, FileStorageService.
- **Acceptance Criteria:**
  - POST/PUT endpoints to create and update a listing (Draft/Pending modes).
  - Validation: Ensure `price > 0`. Allowing `0` leads to spam listings gaming search filters. If "Contact for Price" is needed, use a nullable price and an `is_price_negotiable = true` boolean flag. Ensure valid property type and location.
  - Endpoints to manage listing images. **CRITICAL:** Avoid nightly S3 scanning loops (which cause OOM crashes on massive buckets). Generate **AWS S3 Presigned POST Policies** to a `temp/{uuid}.jpg` prefix. Set an automated S3 Lifecycle Rule to auto-delete anything in `temp/` after 24 hours. **SECURITY WARNING:** The `temp/` prefix must have a strictly **PRIVATE** IAM policy to prevent attackers from using it to host malware.
  - **CRITICAL S3 WORKFLOW & IDOR PREVENTION:** Do not use backend polling, and do NOT blindly trust the S3 object key sent by the frontend. When requesting the Presigned URL, the backend generates the UUID, stores it in a `listing_images` table with `status = UPLOADING` and `created_by = current_user_id`, and returns the URL. The frontend uploads directly to S3. **Only after a 200 OK from S3**, the frontend sends the `POST /api/listings` payload with the UUID. The backend MUST query the DB to verify the UUID belongs to the current user (preventing IDOR attacks). **ORPHAN CLEANUP:** Implement a lightweight `@Scheduled` cron job to hard-delete `listing_images` rows where `status = UPLOADING` and `created_at` is older than 24 hours (for users who abort their upload).
  - **CRITICAL STATE MACHINE & MEDIA OPTIMIZATION:** To prevent "Active" listings with broken images if the async S3 pipeline fails, enforce this strict sequence: The main backend transaction saves the listing as `PENDING_ACTIVATION` and writes an Outbox event. **RACE CONDITION WARNING:** Do NOT use an AWS Lambda triggered by S3 `ObjectCreated:*`, as it may finish and attempt to update the DB before the user's `POST /api/listings` transaction commits. **OOM CRASH WARNING:** Do NOT compress images within the local Spring Boot JVM Outbox listener, as concurrent 10MB uploads will exhaust JVM heap. Mandate **Spring Modulith Event Externalization** (native in Modulith 2.1). The local Modulith listener merely routes the `ListingPendingEvent` to an external message broker (RabbitMQ/SQS). A dedicated background worker executes the `CopyObject` from `temp/` to `active/`, compresses the image, and updates the database status to `ACTIVE`.
  - Assign amenities to a listing.

### Lane 2: Dev 2 (Booking & Interactions)

#### Task 2.1: Viewing Schedules API
- **Tables:** `viewing_schedules`, `listings`
- **Required Layers:** `ViewingScheduleController`, `ViewingScheduleService`, `ViewingScheduleRepository`, DTOs.
- **Acceptance Criteria:**
  - Renter can POST a request to schedule a viewing for an `APPROVED` listing. **CRITICAL:** To survive DST changes without causing Full Table Scans during cron checks, store **both**. The Source of Truth is `scheduled_local_time` and `timezone_id`. However, calculate and store `scheduled_utc_time` and `scheduled_end_utc_time` (`TIMESTAMP WITH TIME ZONE`) at the time of booking and **index it**. Cron jobs query the indexed UTC column for blazing fast reads. **DST DATA CORRUPTION WARNING:** If global timezone databases (tzdata) update, pre-calculated UTC future events will become incorrect. You must either implement an OS/DB tzdata upgrade listener or execute a manual migration to dynamically recalculate the UTC columns globally when DST rules shift.
  - Host/Agent can GET their pending schedules and PUT to confirm/cancel them. **CRITICAL IDOR PREVENTION:** Since the PUT endpoint operates on `scheduleId`, the `@PreAuthorize` security check must explicitly verify that the current user owns the `Listing` attached to the requested `ViewingSchedule`.
  - Ensure logic prevents scheduling in the past. **CRITICAL:** Validations must strictly occur at the *Listing + Time* level, not the Host level, to allow Real Estate Agencies to show multiple properties concurrently via different agents. Enforce a buffer between viewings. As a final safeguard, use a **PostgreSQL Exclusion Constraint**. **IMMUTABLE INDEX WARNING:** Postgres forbids mutable expressions (like `+ interval '1 hour'`) in constraints. You MUST explicitly calculate and store `scheduled_end_utc_time`. The constraint becomes a pure immutable range: `EXCLUDE USING gist (listing_id WITH =, tstzrange(scheduled_utc_time, scheduled_end_utc_time) WITH &&)`. The constraint must be formulated as a **Partial Index** (`WHERE status IN ('PENDING_CONFIRMATION', 'CONFIRMED')`). Using `!= 'DELETED'` is a flaw because it would block slots that were `CANCELLED` or `REJECTED`. **RACE CONDITION WARNING:** If you implement host-level portfolio double-booking prevention (`is_agency = false`), you MUST acquire a pessimistic lock (`SELECT ... FOR UPDATE` on `users`) or use an exclusion constraint on the `host_id`, otherwise concurrent bookings will bypass the check.
  - **CRITICAL:** Implement a `@Scheduled` cron job for past schedules. Do NOT auto-transition straight to `COMPLETED` (which allows renters to review no-show hosts). Transition to `PENDING_COMPLETION` or `REQUIRES_FEEDBACK` (do NOT use `PENDING_CONFIRMATION`, which travels backward in time) and prompt the user. **STATE TRACKING FIX:** To auto-complete/cancel after 48 hours, you MUST add a `confirmation_prompted_at` (`TIMESTAMP`) column to the `viewing_schedules` table. The cron job then looks specifically for `WHERE confirmation_prompted_at < NOW() - INTERVAL '48 hours'`. Relying on `updated_at` or `scheduled_utc_time` will break the logic. MUST use **ShedLock** to prevent duplicate executions.

#### Task 2.2: Favorites & Views API
- **Tables:** `favorites`, `listing_views`
- **Required Layers:** `FavoritesController`, `FavoritesService`, Repositories.
- **Acceptance Criteria:**
  - POST / DELETE to add/remove a listing from favorites for the current user.
  - GET to list the current user's favorite listings.
  - Endpoint to register a "view" on a listing. **CRITICAL:** Buffer view counts in Redis to prevent heavy DB writes. Using Redis Streams or ZSETs for simple view counts is massive over-engineering. Use a simple atomic `INCR views:count:{id}`. A `@Scheduled` cron job reads the keys to flush them to the DB in bulk. **MEMORY LEAK PREVENTION:** Do NOT use `SET key 0 GET`, as it permanently leaves millions of keys with a value of `0` forever, destroying your Redis memory. Instruct Dev 2 to use the native **`GETDEL`** command. It atomically returns the current count and deletes the key. If a user views the listing a millisecond later, the `INCR` command seamlessly recreates the key starting at 1. While this means a server crash post-`GETDEL` might lose a few views, preventing a massive memory leak is far more important for view counts.

#### Task 2.3: Reviews & Reports API
- **Tables:** `reviews`, `viewing_schedules`
- **Required Layers:** `ReviewController`, `ReviewService`, Repositories.
- **Acceptance Criteria:**
  - POST to create a review (requires a `COMPLETED` viewing schedule to prevent fake reviews).
  - GET to list reviews for a listing or a host.
  - POST to report a listing/user. **MODULITH BOUNDARY:** Dev 3 strictly owns the `reports` table. Dev 2's UI should either call Dev 3's `/api/reports` endpoint directly, or Dev 2's backend should publish a `ListingReportedEvent` for Dev 3 to listen to. Do not map the `reports` table in Dev 2. Tenancy/Lease module in Phase 1, True public Property Reviews are explicitly **deferred to Phase 2**.
  - GET feedback for a listing (paginated).
  - Any user can POST a report for a listing (e.g., fraud, inaccurate info).
  - Host can POST a reply to a review.

### Lane 3: Dev 3 (User Management, Admin, Moderation)
- **Tables:** `users`, `reports`, `audit_logs`, `messages`
- **Required Layers:** User/Admin controllers, Notification Service, WebSocket configuration.

#### Task 3.1: User Management, Messaging & Notifications
- **Tables:** `users`, `messages`, `conversations`
- **Required Layers:** `UserController`, `MessageController`, `MessageService`, Repositories, WebSocket Config.
- **Acceptance Criteria:**
  - **CRITICAL:** Dev 3 fully owns the `users` domain. Implement endpoints for `/users/profile` to update contact info, avatars, and check ban status.
  - POST to start a conversation regarding a specific listing.
  - Implement **WebSockets (via Spring STOMP)** for real-time message delivery. **CRITICAL:** SockJS is obsolete technical debt. Drop it completely and use raw STOMP over standard WebSockets. Configure an external STOMP Broker Relay (RabbitMQ/Redis Pub-Sub) for horizontal scaling. **LAYER 7 DOS WARNING:** Allowing the HTTP Upgrade to pass entirely anonymously allows attackers to exhaust your Undertow/Tomcat thread pools. You MUST enforce authentication during the initial HTTP Handshake. **SECURITY FLAW PREVENTION:** Do NOT pass the JWT via a URL query parameter (CWE-598 vulnerability), and do NOT wait to perform authorization inside the STOMP `CONNECT` frame payload (an attacker could open 50k TCP connections and never send the frame, causing a DOS). You must strictly rely on an HTTP `HandshakeInterceptor` using the `Sec-WebSocket-Protocol` header trick to validate the JWT *before* the connection is upgraded. Reject invalid JWTs instantly. **UX TTL WARNING:** Do NOT forcefully drop the TCP connection the exact millisecond the JWT `exp` expires, as this disrupts users typing long messages. Instead, use a STOMP `ChannelInterceptor` to validate the JWT strictly on inbound `SEND` frames, and provide an `/app/auth.refresh` destination so the SPA can update its Principal over the open socket without dropping the TCP connection. If a user is banned mid-session, broadcast a `UserBannedEvent` via Redis Pub/Sub to sever the TCP connection immediately.
  - GET conversations for the current user with unread counts.
  - GET messages within a conversation with pagination.

#### Task 3.2: Notifications API
- **Tables:** `notifications`, `push_tokens`
- **Required Layers:** `NotificationController`, `NotificationService`, Repositories.
- **Acceptance Criteria:**
  - POST to register/update device push tokens (FCM/APNS). **CRITICAL DB BLOAT PREVENTION:** The payload MUST include a unique `device_id` or `installation_id`. Perform an **UPSERT** (update if `device_id` exists, insert if new) to avoid massive database bloat when users log out/in. Furthermore, the backend must catch `UNREGISTERED` Firebase `MessagingException`s and automatically delete dead tokens. **PRIVACY WARNING:** Do not rely solely on Firebase. Provide a `DELETE /api/notifications/tokens/{deviceId}` endpoint and strictly mandate the frontend to call it upon user logout, otherwise logged-out devices will continue receiving highly sensitive notifications.
  - Webhook or internal listener to trigger notifications (e.g., `NewMessageEvent`, `ViewingConfirmedEvent`).
  - GET paginated notification history for the user.

#### Task 3.3: Admin Moderation Dashboard
- **Tables:** `reports`, Envers audit tables (`listings_aud`, `users_aud`)
- **Required Layers:** `AdminController`, `AdminModerationService`, Repositories.
- **Acceptance Criteria:**
  - Admin can GET pending listings.
  - Admin can GET pending reports.
  - **CRITICAL UI EVENTUAL CONSISTENCY WARNING:** The Modulith boundary rules strictly enforce pure Event-Driven Architecture for state changes. Therefore, Dev 3 MUST NOT make synchronous calls to Dev 1 to suspend listings. Dev 3 simply emits an async `ListingSuspensionRequestedEvent`. Because this is async, the Admin UI must gracefully handle eventual consistency (e.g., via Server-Sent Events or WebSockets to push a status update to the dashboard when the listing is fully suspended).
  - **CRITICAL:** Cascading Moderation - When an Admin suspends a listing, Dev 1 receives the event and handles the database update within its own transaction boundary, emitting a `ListingSuspendedEvent`. The Booking module listens to this asynchronously. **CRITICAL SAFETY:** Suspensions are often for severe violations (fraud, safety risks). Therefore, the system MUST immediately cancel all future `CONFIRMED` viewings and notify the renter. **CRITICAL PERFORMANCE:** Suspending an agency listing might cancel hundreds of viewings. Doing this in a single listener might time out the database transaction. This specific event requires **asynchronous batching** or a reliable Message Queue (RabbitMQ/Redis) to handle gracefully.
  - Read-only endpoint to fetch recent audit logs. **CRITICAL:** Do NOT write manual insert statements. Note that Spring Data JPA Auditing only populates `@CreatedBy` columns; it does NOT write standalone log rows. To automatically capture full before/after states, you must use **Hibernate Envers** (`@Audited`). Because Envers creates a separate table for every audited entity, **drop the generic `audit_logs` table from the schema plan**. The Flyway migrations must instead explicitly create `listings_aud`, `users_aud`, etc., to natively support Envers. **MODULITH ENCAPSULATION CRITICAL:** Dev 3 (Admin) CANNOT query `listings_aud` directly using `AuditReader.getRevisions(Listing.class, id)`. Referencing Dev 1's internal `@Entity` triggers a `ModulithException` and fails the build. Querying via raw SQL introduces severe schema coupling. Instead, Dev 1 MUST expose a `ListingAuditApi` interface (returning immutable `ListingAuditSnapshot` DTOs) within its own module. Dev 3's Admin controller should orchestrate read-calls to the respective module APIs to aggregate logs while maintaining strict encapsulation.

---

## 3. Architectural & Coding Alignment

All developers must strictly align their implementation with the existing `day1` demo project and the `Java_Coding_Styles_YOEDU.txt` guidelines:

### Project Structure Alignment (Based on `day1` demo)
- **Controllers & API Docs:** Placed in `org.example...controllers`. Must be annotated with `@RestController` and `@RequestMapping("/api/...")`. All responses must be wrapped in `ApiResponse<T>`. **CRITICAL:** Use `springdoc-openapi` annotations (`@Tag`, `@Operation`) on all controllers/endpoints so the frontend team and other devs have an auto-generated Swagger UI.
- **Services:** Placed in `org.example...service`. Define interfaces here, and their implementations in the `impl` sub-package. Inject dependencies using `@RequiredArgsConstructor`.
- **Modulith Read/Write Boundaries:** Materialized read-models (CQRS data duplication) within a single database is over-engineering for a Phase 1 Modulith. **CRITICAL:** Use pure Event-Driven Architecture (Application Events) strictly for **state changes (writes)**. For **reads**, allow inter-module method calls. **CYCLIC DEPENDENCY WARNING:** Spring Modulith forbids circular dependencies. Establish a strict hierarchy. If Dev 2 needs to read from Dev 1, Dev 1 must expose a `ListingApi` interface and an immutable `ListingSnapshot` record within its *own* module. Do NOT push domain DTOs into a shared `common` package (this creates a God Module anti-pattern).
- **Entities:** Extend `BaseEntity` or `AuditableEntity` (from `domain` package) to automatically manage IDs, `created_at`, and `updated_at`.
- **Shared Utilities:** Cross-cutting concerns (`ApiResponse<T>`, Custom Exceptions) must reside in the `common` or `domain` packages to prevent circular dependencies.
- **Security:** Do not rely purely on rigid `role` columns (since a RENTER could also be an OWNER). Emphasize **Resource-Based Authorization** (e.g., `@PreAuthorize("@securityService.isOwner(#listingId)")`) to handle dynamic access control.

### JPA, Database, and Performance Best Practices
- **Concurrency & Optimistic Locking:** **CRITICAL:** Apply `@Version` for Optimistic Locking to highly mutable core entities (`listings`, `viewing_schedules`, `users`). This prevents the "Lost Update" anomaly. Map `ObjectOptimisticLockingFailureException` to an HTTP 409 Conflict. Do NOT apply `@Version` to append-only logs (`messages`, `audit_logs`) as it wastes database space and CPU.
- **DTO Mapping Boundaries:** **CRITICAL:** Entity-to-DTO mapping MUST occur completely inside the `@Transactional` Service layer. If mapping bleeds into the Controller, Jackson will trigger fatal `LazyInitializationException`s. Controllers must only touch POJOs.
- **Preventing N+1 Queries & Memory Bombs:** Always use `FetchType.LAZY`. **CRITICAL:** If you combine `JOIN FETCH` on a `OneToMany` collection with Spring Data `Pageable` limits, Hibernate will output `HHH000104: applying in memory!`. It will load the entire table into JVM heap, causing an instant OOM crash. You MUST use `@Fetch(FetchMode.SUBSELECT)` or `@BatchSize` for OneToMany relationships when paginating. Reserve `JOIN FETCH` strictly for single entities or non-paginated lists. **GLOBAL BATCH FIX:** To save developers from manually annotating every collection, add `spring.jpa.properties.hibernate.default_batch_fetch_size=100` to `application.yml` to automatically batch fetch lazy collections across the entire platform.
- **Soft Deletions, Unique Indexes, & GDPR:** Implement soft-deletes using a `deleted_at` timestamp. Ensure unique indexes explicitly ignore soft-deletes (e.g., `CREATE UNIQUE INDEX... WHERE deleted_at IS NULL`) so users can re-register without collision. **CRITICAL:** Mandate firing Domain Events upon soft deletion (e.g., `ListingDeletedEvent`). **CRITICAL PRIVACY vs ENVERS:** Indefinite soft deletion violates GDPR (Right to be Forgotten). Implement a scheduled process to permanently purge or anonymize PII after a retention period. Because Hibernate Envers retains history indefinitely in append-only `_aud` tables, standard JPA deletes will NOT comply with GDPR. Dev 3's purge process MUST issue native SQL queries to scrub/anonymize records within the `users_aud` and `REVINFO` tables.
- **Standardized Pagination:** Admin and internal paginated endpoints must accept Spring Data's `Pageable` object and return a standardized wrapped response (`ApiPagedResponse<T>`). **PERFORMANCE MICRO-OPTIMIZATION:** For public feeds and FTS endpoints, Dev 1 MUST return Spring Data's `Slice<T>` instead of `Page<T>`. `Page<T>` forces a costly `SELECT COUNT(*)` query. `Slice<T>` simply fetches `LIMIT + 1` to determine `hasNext()`, saving massive DB CPU cycles. **CRITICAL MEMORY TRAP:** `Slice<T>` does NOT protect against the `HHH000104` OOM crash. `JOIN FETCH` on collections is strictly forbidden for *any* paginated return type (`Page` or `Slice`).
- **Caching & Eviction Isolation:** Where applicable, utilize Spring's `@Cacheable`. **CRITICAL:** Cache eviction must respect domain isolation via events. If Dev 3's Admin updates a property type, Dev 3 must publish a `PropertyTypeUpdatedEvent`. Dev 1 listens to this event and evicts its own lookup cache. **DISTRIBUTED CACHE WARNING:** Because Spring Application Events are JVM-local, Dev 1 evicting its cache on Pod A will leave stale data on Pod B. You MUST use **Redis** as the centralized Spring Cache manager so that evicting a key instantly drops it globally for all horizontally scaled pods.

### Database Migrations
- **Flyway Strict Rules:** Version prefixes must be timestamped (`V202604011200__Create_listings_table.sql`). **CRITICAL:** Do NOT set `spring.flyway.out-of-order=true` because it causes chaotic production state issues and breaks schema validation in CI/CD pipelines. If Dev 2 merges a migration before Dev 1, Dev 1 must rebase and rename their migration prefix to a timestamp strictly *after* Dev 2's migration. Order matters.
- **Modulith Database Coupling:** **CRITICAL:** Do not use hard Foreign Key constraints (`CONSTRAINT FOREIGN KEY`) across module boundaries. If Dev 2's `viewing_schedules` table references Dev 1's `listings` table, Dev 2 should store `listing_id` as a "soft reference" (a UUID column with an index) but without a hard DB-level constraint. This prevents severe schema coupling and allows Dev 1 to refactor their tables without breaking Dev 2.

### Coding Style Rules (Strictly Enforced)
- **Formatting:** Exactly 2 spaces for indentation. Line limit is 100 characters.
- **Imports:** No wildcard imports (`import java.util.*`). Order imports (static first, then non-static).
- **Naming:** Classes must be `UpperCamelCase`. Methods and variables `lowerCamelCase`. No special prefixes like `mName`.
- **Documentation:** Provide Javadoc fragments for all public classes and methods. Emphasize domain clarity over redundant "returns X" comments.
- **Statements:** Use braces `{}` for all control structures (`if`, `for`), even single-line statements.
- **Automated Enforcement:** The team should implement **Spotless** or **Checkstyle** Maven/Gradle plugins to automatically enforce the `Java_Coding_Styles_YOEDU.txt` rules and fail the build if violated, preventing tedious human arguments during code review.

### Testing & Integration (Strictly Enforced)
- **Cross-Domain Communication & Outbox Pattern:** Direct state mutations within the same domain use synchronous API calls. However, **CRITICAL:** State changes that cascade across module boundaries (e.g., Listing Suspended -> Cancel Bookings) MUST use asynchronous Domain Events (Spring Modulith's `@ApplicationModuleListener`) to prevent strict coupling and distributed transaction rollbacks.
- **Distributed Tracing:** Given the heavy use of asynchronous events and outbox patterns, mandate the use of **Micrometer Tracing** (with Zipkin or Jaeger) to inject Correlation IDs, preventing debugging nightmares across boundaries.
- **Unit Testing:** Every task must include comprehensive unit tests using JUnit 5 and Mockito. Test classes should verify business logic in Services and cover happy/edge cases.
- **Integration Testing:** For complex database operations (e.g., full-text search with `search_vector`), developers must write Integration Tests using `@DataJpaTest` (or `@SpringBootTest`) combined with **Testcontainers** to spin up a real PostgreSQL instance. Mockito cannot effectively validate native queries or PostgreSQL-specific extensions.

### Operational Runbook Reminders (Post-Deployment)
- **WebSocket Reverse Proxy:** Ensure that your API Gateway/Load Balancer (e.g., NGINX or AWS ALB) is configured to explicitly forward the `Sec-WebSocket-Protocol` header. If the proxy strips it, the handshake will fail and STOMP connections will be rejected.
- **OS tzdata Updates:** For Task 2.1 (DST Rule changes), ensure an operational runbook is created. When governments arbitrarily change daylight savings rules, you will need a manual SQL migration to dynamically recalculate the `scheduled_utc_time` and `scheduled_end_utc_time` columns based on the updated tzdata package on the Postgres server.
