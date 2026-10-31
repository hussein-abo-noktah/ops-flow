# OpsFlow

OpsFlow is an operations management platform for managing incidents, teams, and users. It includes a React and TypeScript frontend, a Spring Boot backend, and a PostgreSQL database. The project is under development.

## Run with Docker

Install Docker Desktop with Linux containers enabled. The commands below use PowerShell 7 and run from the repository root.

### 1. Create your local configuration

For the first run, copy the example environment file:

```powershell
Copy-Item .env.example .env
```

You can change the local ports and database settings in `.env`.

### 2. Generate authentication keys

Run once. Reuse the existing keys if you have already created them.

```powershell
$keyFolder = Join-Path (Get-Location) 'secrets'
New-Item -ItemType Directory -Force -Path $keyFolder | Out-Null
if ((Test-Path "$keyFolder/jwt-private.pem") -or (Test-Path "$keyFolder/jwt-public.pem")) {
    throw 'Key files already exist. Reuse them instead of overwriting them.'
}
$rsa = [System.Security.Cryptography.RSA]::Create(3072)
try {
    [System.IO.File]::WriteAllText("$keyFolder/jwt-private.pem", $rsa.ExportPkcs8PrivateKeyPem())
    [System.IO.File]::WriteAllText("$keyFolder/jwt-public.pem", $rsa.ExportSubjectPublicKeyInfoPem())
} finally {
    $rsa.Dispose()
}
```

### 3. Start the application

```powershell
docker compose up --build -d
```

Wait for the backend to finish starting and apply the database migrations. Check startup logs with `docker compose logs -f backend`.

- Frontend: [localhost:3000](http://localhost:3000)
- Backend: [localhost:8080](http://localhost:8080)

### 4. Create the local admin account

After the backend has started, run:

```powershell
Get-Content -Raw backend/ops-flow-backend/src/main/resources/db/seed/admin.sql |
    docker compose exec -T db sh -c 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

Sign in with these **local development only** credentials:

- Email: `admin@opsflow.local`
- Password: `OpsFlow@Admin123!`

The seed can be rerun without changing an existing account.

### Stop the application

```powershell
docker compose down
```

Your database data is preserved between runs.

## Run the frontend for development

With the Docker backend running, install Node.js 24 and run:

```powershell
cd frontend/ops-flow-ui
npm ci
npm run dev
```

Open [localhost:5173](http://localhost:5173). API requests are forwarded to the backend on port 8080.
