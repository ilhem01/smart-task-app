$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

Write-Host "== Starting Postgres (Docker Compose) =="
docker compose up -d postgres

Write-Host "== Waiting for Postgres to be ready =="
for ($i = 0; $i -lt 60; $i++) {
  docker compose exec -T postgres pg_isready -U postgres 2>$null | Out-Null
  if ($LASTEXITCODE -eq 0) { break }
  Start-Sleep -Seconds 2
}

Write-Host "== Ensuring database taskdb exists =="
$sql = Get-Content -Raw (Join-Path $Root "docker\postgres\ensure-taskdb.sql")
$sql | docker compose exec -T postgres psql -U postgres -d postgres -v ON_ERROR_STOP=1
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

Write-Host "== Done. Run services with: mvn spring-boot:run (uses localhost:5433 from application.properties) =="
