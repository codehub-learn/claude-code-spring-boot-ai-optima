-- Every-startup script (mounted at /container-entrypoint-startdb.d, run as SYSDBA against the CDB on each container start).
-- Creates spring-ai-training's own schema user (unit 9.2) next to spring-boot-training's APP_USER `training`: one Flyway history
-- per schema. Idempotent (Oracle 23ai IF NOT EXISTS; re-granting is a no-op), so it also creates the user on a database volume
-- that already existed before this script, where the first-boot /container-entrypoint-initdb.d hook will never run again.
ALTER
SESSION
SET CONTAINER = FREEPDB1;

CREATE USER IF NOT EXISTS ai_training IDENTIFIED BY "ai_training" DEFAULT TABLESPACE users QUOTA UNLIMITED ON users;

-- The 23ai developer role (create session, table, view, sequence, procedure, ...), the same grant gvenzl gives APP_USER.
GRANT DB_DEVELOPER_ROLE TO ai_training;