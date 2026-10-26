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
is built and tested with Maven and Java 25, then runs as a non-root user.

The backend currently includes Spring Security's default authentication. Its generated
development password appears in `docker compose logs backend`. No application API or
database persistence layer is implemented yet; Compose supplies datasource environment
variables for future JDBC/JPA integration.

```sh
docker compose logs -f
docker compose down
```

`docker compose down` retains database data. To intentionally delete it, use
`docker compose down --volumes`.
