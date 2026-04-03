Recommended local workflow (single Postgres = Docker Compose)

1) From repo root, run ONE of:
   - Windows:  powershell -ExecutionPolicy Bypass -File scripts/setup-local-db.ps1
   - Git Bash / WSL / macOS / Linux:  bash scripts/setup-local-db.sh

   This starts Postgres and creates "taskdb" if it is missing (idempotent).

2) Start only Postgres is enough for mvn spring-boot:run:
   docker compose up -d postgres

3) Run microservices on the host (default JDBC in application.properties):
   - localhost:5433  -> matches docker-compose "5433:5432" mapping
   - auth_db is created by POSTGRES_DB; taskdb by init script + ensure-taskdb.sql

4) In Docker, Compose sets SPRING_DATASOURCE_URL to postgres:5432 — no change needed.

If you insist on a native PostgreSQL on localhost:5432 instead, set:
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/taskdb   (task-service)
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/auth_db  (auth-service)
   and create auth_db + taskdb manually on that server.
