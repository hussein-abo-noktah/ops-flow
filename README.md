# ops-flow
Operation Management Platform

## Run with Docker Compose

Install Docker with Compose support and start Docker Desktop (using Linux containers).
From the repository root, run:

```sh
docker compose up --build -d
```

- Frontend: http://localhost:3000
- Backend: http://localhost:8080
- PostgreSQL: available internally at `db:5432`; data persists in a named volume.

Compose uses local development defaults. To customize ports or database credentials,
copy `.env.example` to `.env` and edit it before the first start. Use a strong password
for any shared deployment. Changing credentials after database initialization also
requires updating the existing database credentials.

The frontend is built with Node 24 and served by Nginx with React route fallback.
Requests to `/api/` are forwarded to the backend with the path preserved. The backend
is built with Maven and Java 25, then runs as a non-root user.

The backend currently includes Spring Security's default authentication. Its generated
development password appears in `docker compose logs backend`. API requests require
authentication; POST and PUT requests also require a CSRF token under the current
default security configuration.

```sh
docker compose logs -f
docker compose down
```

`docker compose down` retains database data. To intentionally delete it, use
`docker compose down --volumes`.

## Backend module structure

Backend features use packages grouped by module, with these layers inside each module:

- `domain`: entities and domain enums.
- `application`: services, business rules, and mappers.
- `infrastructure/persistence`: Spring Data repositories.
- `api`: REST controllers, with DTOs in `request` and `response` subpackages.

The Organization module follows this structure. Use the same layout when implementing
the remaining modules. This is a layered arrangement; application services currently
use API DTOs and Spring Data repositories directly.

## Organization API

The backend persists organizations in PostgreSQL and runs Flyway migrations at startup.

| Method | Endpoint | Behavior |
| --- | --- | --- |
| POST | `/api/organizations` | Create an ACTIVE organization; returns 201 and a Location header |
| GET | `/api/organizations/{id}` | Read an organization by UUID |
| GET | `/api/organizations?page=0&size=20` | List organizations ordered by creation time and ID |
| PUT | `/api/organizations/{id}` | Replace name, slug, and status |

Create request:

```json
{"name":"Ops Flow","slug":"ops-flow"}
```

Update request:

```json
{"name":"Ops Flow","slug":"ops-flow","status":"SUSPENDED"}
```

Names and slugs are trimmed before validation. Names are required and limited to 150
characters. Slugs are required, unique, limited to 100 characters, and accept lowercase
letters and digits with single separating hyphens. Update status is required and accepts
`ACTIVE`, `SUSPENDED`, or `DISABLED`. Page starts at zero; size must be between 1 and 100.
Responses include `id`, `name`, `slug`, `status`, `createdAt`, and `updatedAt`.
List responses include `content` and a `page` object with `size`, `number`,
`totalElements`, and `totalPages`.

Errors use Problem Details JSON: invalid input returns 400 (with an `errors` field for
body validation), missing organizations return 404, and duplicate slugs return 409.

## Backend integration tests

With Java 25+ and Docker running, execute from `backend/ops-flow-backend`:

```powershell
.\mvnw.cmd clean verify
```

On Linux or macOS, use `./mvnw clean verify`. Failsafe runs `OrganizationIntegrationIT`
against an isolated PostgreSQL 17 Testcontainers database with all Flyway migrations
and Hibernate schema validation enabled. Tests cover persistence, pagination, updates,
validation, conflicts, missing records, malformed requests, authentication, and CSRF.
The application context smoke test is included in this integration suite. `package`
builds the application; `verify` also executes the integration suite and requires Docker.
