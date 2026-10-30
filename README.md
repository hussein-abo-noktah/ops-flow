# ops-flow

Operation Management Platform, currently under development.

## Project contents

- `backend/ops-flow-backend`: Spring Boot 4.1.2, Java 25, Maven Wrapper, Spring MVC, Spring Security, JPA, Flyway, and PostgreSQL.
- `frontend/ops-flow-ui`: React 19 and Vite 8 application with login, session refresh, and a responsive, permission-aware app shell. Module management screens are planned.
- `compose.yaml`: Nginx frontend, backend, and PostgreSQL 17 with a persistent volume. Nginx forwards `/api/` to the backend.
- `.github/workflows/ci.yml`: independent backend verification and frontend lint/build jobs using Java 25 and Node 24.

Backend packages are grouped by feature, with `api`, `application`, `domain`, and `infrastructure/persistence` layers. Auth also has `security`. HTTP APIs exist for authentication, organizations, users, teams, team membership, and incidents. The audit package currently contains its domain model.

## Local setup

Requirements: Java 25 or newer, Node 24 for frontend work, and Docker running Linux containers for PostgreSQL and integration tests.

Copy `.env.example` to `.env`. Generate an RSA key pair once, from the repository root (PowerShell 7):

```powershell
$keyFolder = Join-Path (Get-Location) 'secrets'
New-Item -ItemType Directory -Force -Path $keyFolder | Out-Null
if ((Test-Path "$keyFolder/jwt-private.pem") -or (Test-Path "$keyFolder/jwt-public.pem")) {
    throw 'Key files already exist; do not overwrite the active signing key.'
}
$rsa = [System.Security.Cryptography.RSA]::Create(3072)
try {
    [System.IO.File]::WriteAllText("$keyFolder/jwt-private.pem", $rsa.ExportPkcs8PrivateKeyPem())
    [System.IO.File]::WriteAllText("$keyFolder/jwt-public.pem", $rsa.ExportSubjectPublicKeyInfoPem())
} finally {
    $rsa.Dispose()
}
```

The private key must be an unencrypted PKCS#8 PEM (`BEGIN PRIVATE KEY`); the public key must be X.509 PEM (`BEGIN PUBLIC KEY`). Use RSA keys of at least 2048 bits. Startup fails for unreadable, malformed, weak, or mismatched keys. Keep the private key restricted to the backend. `secrets/` and PEM files are ignored by Git; never commit a deployment private key.

Compose reads `.env` automatically and mounts `AUTH_JWT_KEY_DIR` (default `./secrets`) read-only at `/run/secrets/jwt`. The files must be named `jwt-public.pem` and `jwt-private.pem`, and readable by the container's `app` user. Key files are not bundled into the application image. Direct Maven runs require environment variables to be exported into the shell; Spring Boot does not automatically load the root `.env`.

Start the stack from the repository root:

```sh
docker compose up --build -d
```

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- PostgreSQL: internal to Compose at `db:5432`; data persists in a named volume.

Ports and database credentials can be changed through `.env`. Changing credentials after database initialization also requires updating the existing database. Compose builds the frontend with Node 24 and serves it through Nginx with React route fallback. The backend image uses Java 25 and runs as a non-root user.

```sh
docker compose logs -f
docker compose down
```

Stopping Compose retains the database volume. `docker compose down --volumes` deletes its data.

For frontend development, run from `frontend/ops-flow-ui`:

```sh
npm ci
npm run dev
npm run lint
npm test
npm run typecheck
npm run build
```

### Frontend app shell

Authenticated pages share a collapsible desktop sidebar, a sticky header with breadcrumbs and sign out, and a mobile navigation drawer below 801px. The drawer supports keyboard focus containment, Escape, outside-click dismissal, and focus restoration. Selecting a route or resizing to desktop closes it. A skip link and focused main content support keyboard navigation.

The overview (`/`) links to the modules available to the current user. Menu visibility, module cards, direct route access, and `Can` controls share the same permission helper. `ROLE_PLATFORM_ADMIN` bypasses frontend permission requirements, consistent with backend permission checks.

| Route | Access |
| --- | --- |
| `/` | Any authenticated user |
| `/incidents` | `incident:view` |
| `/teams` | Any authenticated user; team reads have no separate backend permission |
| `/users` | `user:view` |
| `/audit-logs` | `audit:view` |

Restricted direct URLs display an access-denied page within the shell; unknown URLs display a not-found page. Module routes currently display explicit placeholders, without fetching or displaying operational data. Frontend visibility checks support navigation; backend endpoint authorization remains responsible for protecting data.

`npm test` covers permission decisions using Node 24's built-in test runner. `npm run typecheck` validates the application and Vite configuration.

The login page is available to signed-out users. Signed-in users visiting `/login`
are redirected to the dashboard (`/`), after session restoration completes.
Access tokens live in Redux memory (`auth.accessToken`) and are attached to API
requests as bearer tokens. Only the rotating refresh token is persisted in Session
Storage under `opsflow.refreshToken`; reloading obtains a new access token through
`/api/auth/refresh`. Successful sign out revokes the refresh token and clears both
the stored refresh token and in-memory authentication state.

For a direct backend run, set absolute key resource URLs before changing directories (PowerShell, from the repository root):

```powershell
$env:AUTH_JWT_PUBLIC_KEY = ([System.Uri](Join-Path (Get-Location) 'secrets/jwt-public.pem')).AbsoluteUri
$env:AUTH_JWT_PRIVATE_KEY = ([System.Uri](Join-Path (Get-Location) 'secrets/jwt-private.pem')).AbsoluteUri
```

Provide a reachable PostgreSQL database and export `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`. The default URL is `jdbc:postgresql://localhost:5432/ops_flow`. Then run from `backend/ops-flow-backend`:

```powershell
.\mvnw.cmd spring-boot:run
```

On Linux/macOS, use `./mvnw`. Flyway applies migrations at startup; Hibernate validates the resulting schema.

## Authentication API

Auth uses stateless bearer JWTs and opaque refresh tokens submitted explicitly as JSON. It does not use browser cookies, HTTP sessions, HTTP Basic, or form login. There is no CSRF endpoint; CSRF is disabled for this token-based API.

| Method | Endpoint | Request / result |
| --- | --- | --- |
| POST | `/api/auth/login` | `{email, password}`; returns `AuthResponse` |
| POST | `/api/auth/refresh` | `{refreshToken}`; returns a new `AuthResponse` and consumes the old refresh token |
| POST | `/api/auth/logout` | `{refreshToken}`; revokes that token and returns 204; unknown/already revoked tokens are safe to repeat |
| POST | `/api/auth/logout-all` | Requires bearer JWT; revokes all of that user's refresh tokens and returns 204 |
| GET | `/api/auth/me` | Requires bearer JWT; returns `userId`, `organizationId`, and `authorities` |

Login, refresh, and logout are public POST endpoints. GET `/actuator/health` is public. All remaining requests require authentication. The organization and user APIs currently require authentication only; tenant scoping and administrator-only endpoint policies are not implemented.

`AuthResponse` contains:

```json
{
  "accessToken": "<signed JWT>",
  "refreshToken": "<opaque token>",
  "tokenType": "Bearer",
  "expiresIn": 600
}
```

`expiresIn` is the configured access-token lifetime in seconds. Token responses use `Cache-Control: no-store`. Login validates email/password input, finds emails case-insensitively, verifies the existing BCrypt password hash, and updates `lastLoginAt`. Users must be ACTIVE and belong to an ACTIVE organization to log in or refresh.

Access tokens use RS256 (private-key signing, public-key verification) and carry `iss`, `aud`, `sub` (user UUID), `iat`, `exp`, `jti`, `org_id`, `token_type=access`, and `authorities`. Validation checks signature, issuer, audience, timestamps, token type, and required identity claims. Roles belonging to the user's organization become `ROLE_<code>` authorities; their permission codes are included directly, such as `team:update`. `ROLE_PLATFORM_ADMIN` is emitted only for a system role with that code.

Access-token validation is stateless. Logout, logout-all, account status changes, and role changes do not invalidate an issued access token; it remains valid until expiry. Login and refresh reload current user information. No session table or access-token denylist is implemented.

Refresh tokens contain 64 cryptographically random bytes encoded as URL-safe Base64. Only SHA-256 hashes are stored. Rotation locks the original token row, creates its replacement, revokes the original, and links it through `replaced_by` in one transaction. Replacements retain the original absolute expiration (seven days by default). Replaying a revoked refresh token revokes all outstanding refresh tokens for that user, including other logins. Clients should serialize refresh requests. Consumed hashes must be retained until they are no longer needed for replay detection.

Malformed request bodies return 400. Invalid credentials and invalid/expired/revoked refresh tokens return 401 with a generic Problem Details response. Missing or invalid bearer JWTs return 401 through Spring Security. CORS permits only configured origins. Clients must protect both tokens and explicitly send `Authorization: Bearer <accessToken>` on protected requests.

### Auth configuration

| Environment variable | Default / purpose |
| --- | --- |
| `AUTH_JWT_PUBLIC_KEY` | Required for direct runs; `file:` or `classpath:` resource for the X.509 PEM public key |
| `AUTH_JWT_PRIVATE_KEY` | Required for direct runs; `file:` or `classpath:` resource for the PKCS#8 PEM private key |
| `AUTH_JWT_KEY_DIR` | Compose only; host directory containing both PEM files (default `./secrets`) |
| `AUTH_ISSUER` | `https://ops-flow.local` |
| `AUTH_AUDIENCE` | `ops-flow-api` |
| `AUTH_ACCESS_TTL` | `PT10M` |
| `AUTH_REFRESH_TTL` | `P7D` |
| `AUTH_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:5173` |

TTL values use ISO-8601 durations and must be at least one second. Backend instances that issue tokens need the same RSA key pair and database. Services that only verify JWTs need only the public key. Replacing the key pair invalidates existing access tokens. `AUTH_JWT_SECRET` is no longer used. `AUTH_COOKIE_SECURE` is unused because auth does not use cookies.

### Initial administrator

For a local development database, an optional SQL seed is available at
`backend/ops-flow-backend/src/main/resources/db/seed/admin.sql`. Start the backend
once so Flyway has created the schema, then run from the repository root in PowerShell:

```powershell
Get-Content -Raw backend/ops-flow-backend/src/main/resources/db/seed/admin.sql |
    docker compose exec -T db sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Sign in with **admin@opsflow.local** / **OpsFlow@Admin123!**. The seed creates an
ACTIVE development organization, ACTIVE administrator, and system `PLATFORM_ADMIN`
role in one transaction. It can be rerun; an existing email match (case-insensitive)
is left unchanged, including its password and roles. It does not run automatically
and must only be applied to a local development database.

For configurable initial credentials, use the bootstrap runner below.

`auth.application.BootstrapAdmin` is an opt-in startup runner. To provision the first account, set these values in `.env` for Compose, or export them for a direct backend run:

```dotenv
AUTH_BOOTSTRAP_ENABLED=true
AUTH_BOOTSTRAP_EMAIL=admin@example.com
AUTH_BOOTSTRAP_PASSWORD=<choose-a-unique-password>
```

The password must contain at least 12 characters and fit within BCrypt's 72 UTF-8 byte limit. The email is validated, trimmed, and lowercased. No default credentials are supplied.

When the users table is empty, bootstrap creates an ACTIVE organization, an ACTIVE user with a BCrypt password hash, and a system `PLATFORM_ADMIN` role assigned to that user. It creates all records in one transaction and serializes provisioning across backend instances. If any user already exists, bootstrap skips provisioning and never changes their password or roles. Invalid configuration on an empty users table fails startup without creating records.

After the first successful startup, set `AUTH_BOOTSTRAP_ENABLED=false`, remove the bootstrap password, and restart the backend. Bootstrap is disabled by default. It is intended for initial setup; there is no public registration or role-assignment endpoint. The existing HTTP endpoint policies still require authentication only; creating a system role does not add administrator-only endpoint restrictions.

Example, once the backend is running and a user exists:

```sh
curl -H 'Content-Type: application/json' \
  -d '{"email":"alice@example.com","password":"<password>"}' \
  http://localhost:8080/api/auth/login

curl -H 'Authorization: Bearer <access-token>' \
  http://localhost:8080/api/auth/me

curl -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<refresh-token>"}' \
  http://localhost:8080/api/auth/refresh

curl -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<latest-refresh-token>"}' \
  http://localhost:8080/api/auth/logout
```

## Organization API

| Method | Endpoint | Behavior |
| --- | --- | --- |
| POST | `/api/organizations` | Create an ACTIVE organization; returns 201 and a Location header |
| GET | `/api/organizations/{id}` | Read by UUID |
| GET | `/api/organizations?page=0&size=20` | List ordered by creation time and ID |
| PUT | `/api/organizations/{id}` | Replace name, slug, and status |

Create: `{"name":"Ops Flow","slug":"ops-flow"}`.
Update: `{"name":"Ops Flow","slug":"ops-flow","status":"SUSPENDED"}`.

Names/slugs are trimmed. Names are required and limited to 150 characters. Slugs are unique, limited to 100 characters, and use lowercase letters/digits separated by single hyphens. Status accepts ACTIVE, SUSPENDED, or DISABLED. Page starts at zero; size is 1–100. Responses include IDs, fields, and audit timestamps; lists contain `content` and `page` metadata. Invalid inputs return 400, missing records 404, and conflicts 409.

## User API

The user controller provides POST and GET `/api/users`, plus GET and PUT `/api/users/{id}`. Creation accepts `organizationId`, `firstName`, `lastName`, `email`, and `password`. Updates accept the same identity fields plus `status`, without a password. User statuses are ACTIVE, DISABLED, LOCKED, and PENDING. Emails are normalized and checked case-insensitively for duplicates. Passwords are hashed with BCrypt.

## Team API

All permission codes use lowercase resource:action names, such as incident:create, incident:view, user:manage, team:update, and audit:view. This convention applies to permission definitions, database seed data, JWT authorities, and controller checks. Role authorities retain their ROLE_ prefix.

Team operations use the authenticated JWT's org_id; requests do not accept an organization ID. Teams belonging to another tenant return 404, including for platform administrators. Reads require authentication. Creation requires team:create; updates and membership changes require team:update. ROLE_PLATFORM_ADMIN can perform these writes within its own tenant. Permission codes use the same format in storage, JWT authorities, and controller checks.

| Method | Endpoint | Behavior |
| --- | --- | --- |
| POST | /api/teams | Create an ACTIVE team; returns 201 |
| GET | /api/teams/{id} | Read a team within the current tenant |
| GET | /api/teams?page=0&size=20 | Paginated teams within the current tenant |
| PUT | /api/teams/{id} | Update name, code, description, and status |
| POST | /api/teams/{teamId}/members | Add a user; returns 201 |
| GET | /api/teams/{teamId}/members?page=0&size=20 | Paginated members |
| DELETE | /api/teams/{teamId}/members/{userId} | Remove membership; returns 204 |

Create team:

    {"name":"Operations","code":"OPS","description":"Support team"}

Update team:

    {"name":"Operations","code":"OPS","description":"Support team","status":"DISABLED"}

Add member (primary defaults to false):

    {"userId":"<UUID>","primary":true}

Names and codes are trimmed and limited to 150 and 50 characters; descriptions are trimmed and limited to 1000 characters. Codes are uppercased and unique case-insensitively within each tenant. Team status accepts ACTIVE or DISABLED. A user can join several teams in their tenant, but only once per team and with at most one primary team. Adding a member requires an ACTIVE user and an ACTIVE team. Removing a primary membership frees that primary slot; disabling a team preserves its memberships. The membership DELETE path uses the user's UUID. Membership responses include the membership ID, team/user IDs, user names/email, primary flag, and join time.

Invalid input returns 400, missing or foreign resources 404, insufficient permissions 403, and duplicate or inactive membership conflicts 409. Pagination uses zero-based pages and sizes of 1–100. Database constraints and row locks protect membership rules under concurrent requests. Migration V13 adds case-insensitive team code uniqueness and one primary membership per user; existing conflicting data must be resolved before applying it.

## Incident API and workflow

Incidents follow this domain-enforced sequence:

`OPEN → ACKNOWLEDGED → INVESTIGATING → MITIGATED → RESOLVED → CLOSED`

Each command permits only the next transition. Skipping, repeating, or reversing transitions returns 409. There is no status setter or generic status-change endpoint. Updates accept title and description only; severity and assignment have separate commands. CLOSED incidents reject edits, severity changes, and assignment changes.

All operations use the JWT's org_id, including for PLATFORM_ADMIN. Foreign or missing resources return 404. Every endpoint requires its permission or ROLE_PLATFORM_ADMIN within the current tenant.

| Method | Endpoint | Permission / behavior |
| --- | --- | --- |
| POST | `/api/incidents` | incident:create; opens an incident, returns 201 and Location |
| GET | `/api/incidents/{id}` | incident:view |
| GET | `/api/incidents?page=0&size=20` | incident:view; paginated tenant incidents |
| PUT | `/api/incidents/{id}` | incident:update; replaces title and description |
| POST | `/api/incidents/{id}/assign` | incident:assign; replaces team/user assignment |
| POST | `/api/incidents/{id}/severity` | incident:update |
| POST | `/api/incidents/{id}/acknowledge` | incident:update; OPEN → ACKNOWLEDGED |
| POST | `/api/incidents/{id}/start-investigation` | incident:update; ACKNOWLEDGED → INVESTIGATING |
| POST | `/api/incidents/{id}/mitigate` | incident:update; INVESTIGATING → MITIGATED |
| POST | `/api/incidents/{id}/resolve` | incident:resolve; MITIGATED → RESOLVED |
| POST | `/api/incidents/{id}/close` | incident:resolve; RESOLVED → CLOSED |
| GET | `/api/incidents/{id}/timeline?page=0&size=20` | incident:view; oldest events first |

Create: `{"title":"API outage","description":"Elevated error rate","severity":"SEV1"}`.
Update: `{"title":"API outage","description":"Investigation notes"}`.
Severity: `{"severity":"SEV2"}`. Values are SEV1, SEV2, SEV3, SEV4.
Assignment: `{"teamId":"<UUID>","userId":"<UUID>"}`; either field may be null, and `{}` clears both. Assignees must be active and belong to the incident's tenant. Assigning a user does not require team membership.
Workflow commands take no body.

Titles are trimmed, required, and limited to 250 characters; optional descriptions are trimmed and limited to 10,000 characters. Incident numbers are generated as INC- plus a UUID and protected by the existing tenant/number unique constraint. Page starts at zero and size is 1–100.

Creation and successful changes append timeline entries with the actor, timestamp, and old/new values in the same transaction. Detail and assignment values are JSON objects stored as text; status and severity values are enum names. Every transition, including resolution, uses STATUS_CHANGED. Unchanged updates create no event. Acknowledgement, mitigation, resolution, and closure set their corresponding timestamps. Optimistic locking protects concurrent writes; a losing request returns 409 and rolls back its timeline entry. The response includes the current entity version.

Incident tests cover the entire transition matrix, timestamps, closed-state edits, workflow HTTP commands, validation, permissions, tenant isolation, assignments, no-op events, concurrent acknowledgement, and transaction rollback using PostgreSQL.

### Incident search, filtering, and sorting

GET `/api/incidents` combines optional filters with AND through reusable JPA Specifications. The organization predicate always comes from the authenticated JWT and applies to both results and total counts. The response keeps the existing `content` and `page` structure.

| Parameter | Meaning |
| --- | --- |
| `q` | Case-insensitive literal substring across title, description, or incident number; trimmed, maximum 250 characters; blank means no text filter |
| `status` | One of OPEN, ACKNOWLEDGED, INVESTIGATING, MITIGATED, RESOLVED, CLOSED |
| `severity` | One of SEV1, SEV2, SEV3, SEV4 |
| `assignedTeamId` / `assignedUserId` | Exact assignee UUID |
| `createdBy` | Exact creator UUID |
| `createdFrom` / `createdTo` | Inclusive creation timestamp bounds; ISO 8601 timestamps with a timezone, e.g. 2026-10-01T00:00:00Z |
| `page` / `size` | Zero-based page (default 0), size 1–100 (default 20) |
| `sortBy` | id, incidentNumber, title, severity, status, createdAt, updatedAt, acknowledgedAt, mitigatedAt, resolvedAt, closedAt; default createdAt |
| `direction` | ASC or DESC; default ASC |

Example:

```text
GET /api/incidents?q=database&status=INVESTIGATING&severity=SEV1&createdFrom=2026-10-01T00:00:00Z&page=0&size=20&sortBy=createdAt&direction=DESC
```

Text search treats %, _, and backslash as literal characters. Missing filters include unassigned incidents. Unknown or foreign filter UUIDs return an empty result. Invalid enums, UUIDs, timestamps, sort fields, date ranges, or pagination return 400 ProblemDetail. The date range may be open-ended; createdFrom must not exceed createdTo. Page offsets must fit within the database pagination limit.

Sorting appends id ASC to break ties unless id itself is selected. Enum sorting uses stored names (status alphabetically, severity SEV1 through SEV4); nullable timestamp sorts use PostgreSQL's default null ordering. Pagination is stable for unchanged data; concurrent writes can shift offset-based pages. Migration V14 adds an organization/createdAt/id index for the default listing order and creation-date filters.

Search integration tests cover combined and independent filters, tenant isolation with matching foreign data, literal text search, inclusive date bounds, deterministic pagination, sort direction, empty pages, and invalid query parameters on PostgreSQL.

### Incident comments and immutable collaboration history

Comments remain available after incident closure for follow-up discussion. An active user needs incident:update (or ROLE_PLATFORM_ADMIN) to add a comment. Only its author may edit it, including when the caller is a platform administrator. Reads require incident:view or ROLE_PLATFORM_ADMIN. Every endpoint is scoped to the JWT organization and the incident in the URL; foreign or mismatched resources return 404.

| Method | Endpoint | Behavior |
| --- | --- | --- |
| POST | /api/incidents/{incidentId}/comments | Add comment; returns 201 and Location |
| GET | /api/incidents/{incidentId}/comments | Paginated comments, oldest first |
| GET | /api/incidents/{incidentId}/comments/{commentId} | Current content, author, edited flag, version, timestamps |
| PUT | /api/incidents/{incidentId}/comments/{commentId} | Author-only edit with expected version |
| GET | /api/incidents/{incidentId}/comments/{commentId}/history | Paginated immutable revisions in revision-number order |

Create body: {"comment":"Investigating database connections"}.
Edit body: {"comment":"Connection pool exhaustion confirmed","version":0}.

Comment text is trimmed, required, and limited to 10,000 characters. Each new comment starts at version/revision 0; each edit creates a new immutable snapshot and a COMMENT_EDITED timeline event containing the old and new text. COMMENT_ADDED records initial content. Responses expose commentId on linked timeline entries and a revision number in metadata. An unchanged edit creates no revision or event. Missing or negative versions return 400; stale or concurrent edits return 409. There are no comment, revision, or timeline delete endpoints.

Comment lists and histories accept page (default 0) and size (default 20, range 1–100). Timeline queries additionally accept eventType, actorId, commentId, from, to, and direction (ASC by default; DESC supported). Filters combine with AND, timestamps use inclusive ISO 8601 bounds, and both results and total counts remain tenant-scoped. Timeline ordering uses createdAt and id in the requested direction.

Example:

    GET /api/incidents/{id}/timeline?eventType=COMMENT_EDITED&actorId={userId}&commentId={commentId}&from=2026-10-01T00:00:00Z&direction=DESC&page=0&size=20

Migration V15 adds optimistic comment versions, a revision table, comment links on timeline events, and history indexes. PostgreSQL triggers reject UPDATE, DELETE, and TRUNCATE on timeline and revision tables. Comment identity (incident, author, creation time, ID) is fixed; edits must change content, increment the version, and set the edited flag. Deferred database checks require each comment insert/edit to have matching revision content and a timeline event before commit. All three records commit or roll back together. History entities are also marked immutable in Hibernate, and their repositories expose append/read operations only.

Existing comments receive a LEGACY_SNAPSHOT at migration time with their current text and an unknown editor. Earlier text or editor identities cannot be reconstructed. Existing timeline entries are retained. Database owners and superusers can disable triggers; production runtime credentials should be separate from the migration owner and have no DDL privileges. These protections apply to incident collaboration history; the separate audit_logs module remains a domain scaffold.

PostgreSQL integration tests exercise revision history, author-only edits, tenant isolation, query filters, closed-incident discussion, input validation, stale/concurrent edits, SQL tampering, deferred history checks, rollback after timeline failure, and migration of existing comments. Disposable database fixture cleanup uses owner-only trigger changes inside a transaction; application code has no cleanup bypass.

## Verification and CI

From `backend/ops-flow-backend`, run:

```powershell
.\mvnw.cmd clean verify
```

Maven runs unit tests with Surefire and `*IT` integration tests with Failsafe. Integration tests require Docker and use PostgreSQL 17 with Flyway migrations and Hibernate schema validation.

The user and organization integration tests verify that JWT-authenticated writes work without CSRF tokens and anonymous writes return 401. Password persistence is checked using the application's configured `PasswordEncoder`, including password matching and preserving the hash during user updates.

`AuthIntegrationIT` defines an auth-specific application context and covers JSON login/refresh/logout, concurrent rotation, replay revocation, inactive accounts, expiry, JWT validation, CORS, and request validation. `JwtConfigTest` checks RSA PEM loading, key size and pairing, public-key verification, RS256 enforcement, and lifetime configuration. Bootstrap tests cover startup provisioning, login, the system administrator role, repeat/concurrent invocations, and credential validation. Organization and user integration tests remain in their respective packages.

CI runs on pushes, pull requests, and manual dispatch. The backend job runs `clean verify`; the frontend job runs `npm ci`, lint, and production build. Reports and build artifacts are retained for seven days.
