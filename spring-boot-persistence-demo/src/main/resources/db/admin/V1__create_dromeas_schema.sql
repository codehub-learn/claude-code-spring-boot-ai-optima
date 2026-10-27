-- Bootstrap: the dedicated DROMEAS schema (user) for the persistence demo, in the same FREEPDB1 pluggable database the rest of the
-- repo uses. This is the only script that needs a privileged connection, so it lives OUTSIDE db/migration: Boot's Flyway and the
-- Testcontainers tests never see it. Apply it once, as SYSTEM, with its own history table so it never mixes with the app's:
--   ./mvnw -pl spring-boot-persistence-demo flyway:migrate@create-schema
-- Everything inside the schema (tables, indexes, seed and bulk data) is then owned by db/migration and runs as DROMEAS.
--
-- The local credential below is throwaway, the same convention as compose.yaml; override it outside local dev.

declare
	v_count number;
begin
	select count(*) into v_count from dba_users where username = 'DROMEAS';
	if v_count = 0 then
		execute immediate 'create user dromeas identified by dromeas default tablespace users quota unlimited on users';
	end if;
end;
/

grant create session, create table, create sequence, create view, create procedure to dromeas;