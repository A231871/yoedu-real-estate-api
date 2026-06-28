# Welcome to the Real Estate API Database 🚀

Welcome aboard! As a new developer on the Real Estate & Rental Platform, you're stepping into a backend that was engineered from day one for high scale, concurrency, and data integrity. 

This guide serves as your map to our highly advanced PostgreSQL 18-grade database. We don't just use our database as a dumb storage bucket; we actively leverage advanced PostgreSQL features—such as soft-deletes, triggers, materialized views, geo-spatial constraints, and strict monetization tracking—to enforce business rules natively and optimize performance. 

Grab a coffee, and let's dive into the architecture! ☕

---

## 1. Core Domains (The "Big Picture")

To make the system easy to digest, the database is grouped into logical modules matching our Spring Modulith architecture.

### 👤 Auth & Users
This domain manages Authentication, Authorization, KYC (Know Your Customer) compliance, and global auditing.
- **`users`**: The central auth table. We use an array `roles VARCHAR(50)[]` constraint so a single user can act as both an `OWNER` and a `RENTER` simultaneously.
- **`agent_profiles`**: A 1-to-1 extension table linked to `users.id`. It isolates specialized KYC data (like license numbers and ID card scans) without polluting the main `users` table with nullable columns.
- **`owner_profiles`**: A 1-to-1 extension table similar to `agent_profiles`, but tailored for private property owners.
- **`refresh_tokens`**: Manages secure, long-lived session lifecycles for users.
- **`user_auth_tokens`**: Handles short-lived, single-use tokens for actions like password resets and email verification.
- **`audit_logs` (Envers `_aud` tables)**: Tracks before/after row changes for critical entities, capturing who made the change and when, ensuring full GDPR and SOC2 compliance.

### 🏠 Property Management
This is the heart of the application, handling the core product offerings, lookups, and massive read/write traffic.
- **`listings`**: The most complex table in the system. It tracks dynamic pricing, status state machines (`DRAFT`, `PENDING`, `APPROVED`), and geospatial coordinates.
- **`listing_images`**: Manages S3-backed media URLs, using strict `sort_order` and `is_primary` flags to govern the frontend carousel.
- **`listing_amenities`**: The Many-to-Many join table bridging `listings` and `amenities`.
- **`listing_views`**: Tracks unique traffic to listings. Buffered in Redis Streams before flushing to the DB to prevent meltdown.
- **`saved_searches`**: Stores user filter preferences (JSONB) to push alerts when matching properties are listed.
- **`provinces`, `districts`, `wards`**: Hierarchical geographic data representing Vietnam's administrative boundaries.
- **`property_types` & `amenities`**: Static reference data for property characteristics, heavily cached in Redis.

### 💬 Social, Interactions & Messaging
This domain handles peer-to-peer trust, bookings, and real-time communication.
- **`conversations` & `messages`**: Real-time STOMP WebSocket chat persistence. Conversations are strictly bound to a specific `listing_id` context.
- **`viewing_schedules`**: Where renters book visits to properties, regulated by strict timezone rules and state machines.
- **`favorites`**: A simple join table storing user wishlists.
- **`reviews`**: Peer-to-peer trust mechanism. We enforce that a review can only be left if a corresponding `viewing_schedule` is marked as `COMPLETED`.
- **`reports`**: Allows users to report fraudulent listings or abusive hosts.
- **`notifications` & `push_tokens`**: Powers our omnichannel alerts (in-app, email, Firebase Push). Includes `UPSERT` logic to prevent token bloat.

### 💰 Financials & Legal
This domain manages platform revenue, user purchases, and agreements.
- **`listing_packages`**: Defines the available promotional tiers (e.g., "Basic", "Gold", "Diamond") that boost a listing's `priority_level`.
- **`transactions`**: The immutable financial ledger. Records package purchases, payment methods, and timestamps. Linked to `users` and `listings` with strict `ON DELETE RESTRICT` constraints to prevent accidental financial data loss.
- **`contracts`**: Stores legally binding digital rental agreements (PDF hashes/signatures) generated between Owners and Renters after a successful viewing.

---

## 2. Crucial Relationships & Joins

Understanding how these tables connect is critical for writing efficient JPA Queries.

- **`listings` + `listing_images` (One-to-Many):** 
  When building the frontend search feed, you don't want to load all 20 images for a listing. You will `JOIN listing_images ON listings.id = listing_images.listing_id WHERE listing_images.is_primary = TRUE` to fetch only the main thumbnail.
- **`listings` + `users` + `agent_profiles` (Many-to-One):** 
  When displaying a property details page, you need the host's information. You'll `JOIN users` to get their `full_name` and `avatar_url`, and conditionally `JOIN agent_profiles` if you need to display their verified license number.
- **`listings` + `listing_amenities` + `amenities` (Many-to-Many):** 
  To filter properties that have a "Pool" and "WiFi", you must join through the `listing_amenities` junction table.

---

## 3. Advanced Postgres Superpowers (Business Logic)

We rely on PostgreSQL to do heavy lifting that would be dangerous or slow in Java.

### Materialized Views
For complex analytical dashboards (like Admin Revenue Reports or Listing Performance Stats), we utilize **Materialized Views**. Instead of running massive `SUM()` and `COUNT()` aggregations across millions of rows on every page load, PostgreSQL pre-computes these views in the background.

### PostGIS Spatial Search (Geography)
To find "Apartments within 5km of my location," we use the PostGIS extension. We use `geography(Point, 4326)` instead of `geometry` to calculate the curvature of the Earth in **meters**.

### Full-Text Search (FTS)
The `listings` table has a Generated Column called `search_vector`. It automatically concatenates the title, description, and location, strips accents (via `unaccent`), and generates a `tsvector`. We then query against a blazing-fast GIN index for natural language search.

### Exclusion Constraints (Double-Booking Prevention)
Java is susceptible to race conditions. Instead, we use a **PostgreSQL GiST Exclusion Constraint** on `viewing_schedules`. It evaluates `tstzrange(scheduled_utc_time, scheduled_end_utc_time)`. If two overlapping ranges try to insert concurrently for the same `listing_id`, the database outright rejects the second transaction.

---

## 4. Important Database Rules (Gotchas) 🛑

When writing your JPA repositories or modifying the schema, you **MUST** follow these rules to avoid breaking the system:

1. **The Soft-Delete Strategy:**
   We never `DELETE FROM users`. We update the `deleted_at` timestamp. Our unique indexes (like email) explicitly say `WHERE deleted_at IS NULL`, allowing a user to delete their account and re-register the same email a year later without throwing a unique constraint violation.
   
2. **Messaging History (`ON DELETE SET NULL`):**
   When a user permanently deletes their account (GDPR erasure), we do not want to destroy the chat history for the *other* user in the conversation. Therefore, the `messages` table uses `ON DELETE SET NULL` for the `sender_id`. The chat bubble will simply show "Deleted User" while preserving the conversation context.

3. **Refresh Tokens & Row Contention:**
   Our `refresh_tokens` table is designed to prevent database row-lock contention. Instead of constantly `UPDATE`-ing a single user session row every time they open the app, we insert an immutable token row with an `expires_at` timestamp. This allows a user to be logged in seamlessly across their Phone, Tablet, and Laptop simultaneously without transaction deadlocks.
   
4. **Pagination - The "Count" Killer:** 
   Never use Spring Data's `Page<T>` for the public listing feed. Counting millions of records on every scroll event will crash the database. Use `Slice<T>`.

5. **The N+1 OOM Trap:**
   Never use `JOIN FETCH` on paginated One-to-Many relationships. Hibernate will pull the *entire table* into RAM to paginate in-memory (`HHH000104`), causing an immediate Out-Of-Memory crash.

---
*Welcome to the team! If you have any questions regarding the schema, don't hesitate to reach out to the architecture team.*
