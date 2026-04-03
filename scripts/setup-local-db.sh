#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "== Starting Postgres (Docker Compose) =="
docker compose up -d postgres

echo "== Waiting for Postgres to be ready =="
for _ in $(seq 1 60); do
  if docker compose exec -T postgres pg_isready -U postgres >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

echo "== Ensuring database taskdb exists =="
docker compose exec -T postgres psql -U postgres -d postgres -v ON_ERROR_STOP=1 < "$ROOT/docker/postgres/ensure-taskdb.sql"

echo "== Done. Run services with: mvn spring-boot:run (uses localhost:5433 from application.properties) =="
