-- U9.2 - chat memory in Oracle, in spring-ai-training's own schema (ai_training locally, see compose/oracle-startdb).
-- Conventions (CLAUDE.md): snake_case, singular table names, pk_/fk_/ix_/ck_ constraint prefixes. Oracle syntax: varchar2, clob,
-- "timestamp with time zone", systimestamp, "default" before "not null".

-- The application's conversation registry (Spring Data JDBC aggregate conversation.Conversation). A conversation exists from
-- POST /conversations on, before it has any messages, so the id cannot live only in the memory table. turns counts every
-- completed turn, including the ones the memory window has since dropped.
create table conversation
(
	id         varchar2(36)                                  not null,
	created_at timestamp with time zone default systimestamp not null,
	turns      number(10)               default 0            not null,
	version    number(19)               default 0            not null,
	constraint pk_conversation primary key (id)
);

-- Spring AI's JdbcChatMemoryRepository table. Name, columns and types are the library's (its schema-oracle.sql, Spring AI 2.0.1):
-- OracleChatMemoryRepositoryDialect hard-codes them in its SQL, so they cannot follow the naming convention. The library's own
-- schema initializer is off (spring.ai.chat.memory.repository.jdbc.initialize-schema: never); Flyway owns the schema.
-- Added here: the foreign key to conversation, so deleting a conversation deletes its memory in the same statement.
-- Note: Oracle stores '' as NULL, so a message with empty text violates "content not null" (see ConversationService).
create table spring_ai_chat_memory
(
	conversation_id varchar2(36 char) not null,
	content         clob              not null,
	"TYPE"          varchar2(10 char) not null,
	"TIMESTAMP"     timestamp         not null,
	sequence_id     number(19)        not null,
	constraint ck_spring_ai_chat_memory_type check ("TYPE" in ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
	constraint fk_spring_ai_chat_memory_conversation foreign key (conversation_id) references conversation (id) on delete cascade
);

create index ix_spring_ai_chat_memory_conversation_timestamp on spring_ai_chat_memory (conversation_id, "TIMESTAMP");

create index ix_spring_ai_chat_memory_conversation_sequence on spring_ai_chat_memory (conversation_id, sequence_id);