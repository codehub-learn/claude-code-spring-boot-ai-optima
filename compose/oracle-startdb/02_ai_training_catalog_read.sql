-- Every-startup script, after 01_ai_training_user.sql. Unit 10.1: spring-ai-training ingests spring-boot-training's catalog into its
-- vector store, so ai_training may READ (select only, no locks) the two catalog tables in the training schema. Nothing else.
-- The tables exist only after `./mvnw -pl spring-boot-training flyway:migrate`; until then each grant is skipped with a message
-- (ORA-00942), and the next container start (`docker compose restart oracle`) grants it. Re-granting is a no-op.
ALTER
SESSION
SET CONTAINER = FREEPDB1;

SET SERVEROUTPUT ON

BEGIN
FOR t IN (SELECT 'PRODUCT' AS table_name FROM dual UNION ALL SELECT 'CATEGORY' FROM dual)
	LOOP
		BEGIN
EXECUTE IMMEDIATE 'GRANT READ ON training.' || t.table_name || ' TO ai_training';
EXCEPTION
			WHEN OTHERS THEN
				DBMS_OUTPUT.PUT_LINE('Skipped READ on training.' || t.table_name || ': ' || SQLERRM);
END;
END LOOP;
END;
/