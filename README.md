# maddie-west-events-backend

Spring Boot backend for Maddie West Events: wedding rental inventory (arbors, vases, tablecloths, etc.), customer rental requests, the coordinator approval workflow, and the public website contact form.

This is the canonical Maddie West Events backend (it replaces the earlier `madd-west-events-backend` Node/Express service). It uses its own MongoDB database, though it can run against the same Mongo instance (see `../database/docker-compose.yml`).

## Requirements

- Java 21 (the project targets Java 21; Lombok annotation processing is currently flaky on
  newer JDKs such as 25 — if multiple JDKs are installed, point `JAVA_HOME` at a JDK 21
  install before running Maven, e.g. `export JAVA_HOME=$(/usr/libexec/java_home -v 21)`)
- Maven 3.9+
- A running MongoDB instance

## Setup

1. Copy `.env.example` to `.env` and fill in values:

   ```bash
   cp .env.example .env
   ```

   Required values:
   - `MONGODB_URI` — Mongo connection string (defaults to `mongodb://localhost:27017/rental-service`)
   - `JWT_SECRET` — long random string used to sign coordinator JWTs
   - `COORDINATOR_SEED_USERNAME` / `COORDINATOR_SEED_PASSWORD` — initial coordinator login, created on first startup
   - `SMTP_*` / `MAIL_FROM` / `COORDINATOR_NOTIFICATION_EMAIL` — email notifications
   - `CORS_ALLOWED_ORIGINS` — frontend origin (defaults to `http://localhost:5173`)

2. Run the app:

   ```bash
   mvn spring-boot:run
   ```

   The service starts on `http://localhost:8081` (configurable via `PORT`).

3. API docs / Swagger UI: `http://localhost:8081/swagger-ui.html`

## API overview

### Public

- `GET /api/items?category=&name=&page=&limit=` — browse active rental items
- `GET /api/items/available?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD&category=&name=&page=&limit=` — items with `availableQuantity` computed for a date range
- `GET /api/items/{id}?startDate=&endDate=` — item detail, with `availableQuantity` for the given date range if supplied
- `POST /api/rental-requests` — submit a rental request: `{ items: [{itemId, quantity}], dateRange: {startDate, endDate}, requester: {name, email, phone, notes} }`
- `POST /api/auth/login` — coordinator login: `{ username, password }` → `{ token, expiresAt, username, role }`

### Coordinator (JWT required, `Authorization: Bearer <token>`)

- `GET /api/admin/items` / `GET /api/admin/items/{id}` — list/view all items (including inactive)
- `POST /api/admin/items` / `PUT /api/admin/items/{id}` — create/update an item
- `DELETE /api/admin/items/{id}` — soft-delete (sets `active: false`)
- `GET /api/admin/rental-requests?status=&page=&limit=` / `GET /api/admin/rental-requests/{id}` — list/view requests, optionally filtered by status (`PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`)
- `POST /api/admin/rental-requests/{id}/approve` — approve a `PENDING` request; returns `409` with conflict details if there isn't enough stock left for the request's date range
- `POST /api/admin/rental-requests/{id}/reject` — reject a `PENDING` request: `{ reason }`
- `POST /api/admin/rental-requests/{id}/cancel` — cancel a `PENDING` or `APPROVED` request (cancelling an `APPROVED` request restores its reserved availability)

### Rental request lifecycle & emails

1. **Submission** (`POST /api/rental-requests`) — request is created with status `PENDING`. Emails are sent to the coordinator (new request to review) and to the requester (confirmation).
2. **Approval** — coordinator approves via the admin endpoint. Status becomes `APPROVED`, the requested quantities are now counted against `availableQuantity` for overlapping date ranges, and the requester gets an approval email.
3. **Rejection** — coordinator rejects with a reason. Status becomes `REJECTED` and the requester is emailed the reason.
4. **Cancellation** — a `PENDING` or `APPROVED` request can be cancelled. Cancelling an `APPROVED` request immediately frees up its reserved quantities.

Every status change is appended to the request's `statusHistory` (status, timestamp, who changed it, optional reason).

### Frontend integration note

The existing Node backend's `/api/items/available` takes a single `date` query param. This service's equivalent endpoint takes a **date range** (`startDate` and `endDate`), since rentals can span multiple days. The response envelope (`{ success, data, pagination }`) and item shape (`images[]`, `availableQuantity`, `metadata`) match `maddie-west-events`'s existing `RentalItem` type.

## Tests

```bash
mvn test
```

Integration tests use Testcontainers to spin up a temporary MongoDB instance — Docker must be running.
