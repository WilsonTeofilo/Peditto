\getenv app_password POSTGRES_APP_PASSWORD
\getenv migration_password POSTGRES_MIGRATION_PASSWORD
SELECT 'CREATE ROLE peditto_app WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS' WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='peditto_app') \gexec
SELECT format('ALTER ROLE peditto_app WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD %L', :'app_password') \gexec
SELECT 'CREATE ROLE peditto_migrator WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS' WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='peditto_migrator') \gexec
SELECT format('ALTER ROLE peditto_migrator WITH LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS PASSWORD %L', :'migration_password') \gexec
GRANT CONNECT ON DATABASE peditto TO peditto_app, peditto_migrator;
GRANT USAGE ON SCHEMA public TO peditto_app, peditto_migrator;
GRANT CREATE ON SCHEMA public TO peditto_migrator;
DO $$ DECLARE r record; BEGIN
  FOR r IN SELECT tablename FROM pg_tables WHERE schemaname='public' LOOP
    EXECUTE format('ALTER TABLE public.%I OWNER TO peditto_migrator', r.tablename);
  END LOOP;
  FOR r IN SELECT sequencename FROM pg_sequences WHERE schemaname='public' LOOP
    EXECUTE format('ALTER SEQUENCE public.%I OWNER TO peditto_migrator', r.sequencename);
  END LOOP;
END $$;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO peditto_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO peditto_app;
ALTER DEFAULT PRIVILEGES FOR ROLE peditto_migrator IN SCHEMA public GRANT SELECT, INSERT, UPDATE ON TABLES TO peditto_app;
ALTER DEFAULT PRIVILEGES FOR ROLE peditto_migrator IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO peditto_app;

