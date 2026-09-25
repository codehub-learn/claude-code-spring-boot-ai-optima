-- U10.1 - the RAG knowledge base: Spring AI's OracleVectorStore table (Oracle 23ai AI Vector Search).
-- OracleVectorStore's SQL hard-codes the columns (id, content, metadata, embedding); the table name is config
-- (spring.ai.vectorstore.oracle.table-name). This is the library's own DDL (OracleVectorStore.afterPropertiesSet, Spring AI 2.0.1),
-- owned by Flyway instead: spring.ai.vectorstore.oracle.initialize-schema is false.
--
-- vector(1024, FLOAT64): exactly the embedding model's output length (qwen3-embedding:0.6b). A different model means a new
-- migration and re-ingesting: vectors from two models live in different spaces and cannot be compared.

create table knowledge_vector
(
	id        varchar2(36) default sys_guid() not null,
	content   clob                            not null,
	metadata  json                            not null,
	embedding vector(1024, FLOAT64) annotations (Distance 'COSINE', IndexType 'IVF'),
	constraint pk_knowledge_vector primary key (id)
);

-- Approximate nearest-neighbour index (IVF: vectors clustered into partitions; a search probes the closest ones). Same
-- definition the library would create. A top-k search only uses it with FETCH APPROX; exact search (the store's default,
-- search-accuracy unset) scans the table, which is what a few hundred rows want anyway.
create vector index ix_knowledge_vector_embedding on knowledge_vector (embedding)
	organization neighbor partitions
	distance COSINE
	with target accuracy 95
	parameters (type IVF, neighbor partitions 10);