-- Idempotent: safe to run on every local setup (host pipes this into psql).
DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'taskdb') THEN
    CREATE DATABASE taskdb;
  END IF;
END
$$;
