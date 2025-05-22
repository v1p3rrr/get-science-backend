DO
$do$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_database WHERE datname = 'GetScienceDb') THEN
      PERFORM dblink_exec('dbname=postgres', 'CREATE DATABASE GetScienceDb');
   END IF;
END
$do$;

DO
$do$
BEGIN
   IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'commonuser') THEN
      CREATE USER commonuser WITH PASSWORD '1';
      GRANT ALL PRIVILEGES ON DATABASE GetScienceDb TO commonuser;
   END IF;
END
$do$;