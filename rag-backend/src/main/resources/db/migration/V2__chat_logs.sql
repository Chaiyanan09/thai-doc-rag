-- Every question and answer from /api/chat, for reviewing answers and measuring quality later
CREATE TABLE chat_logs (
    id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    question         text        NOT NULL,
    answer           text        NOT NULL,
    found            boolean     NOT NULL,             -- false = "not found in the documents"
    source_chunk_ids bigint[]    NOT NULL DEFAULT '{}', -- chunks sent to the LLM, in rank order ([1], [2], ...)
    cited_chunk_ids  bigint[]    NOT NULL DEFAULT '{}', -- chunks the answer actually cited
    model            text,
    retrieval_ms     integer     NOT NULL,
    llm_ms           integer     NOT NULL,
    created_at       timestamptz NOT NULL DEFAULT now()
);
-- No foreign key to chunks on purpose: logs stay even after a document is deleted

CREATE INDEX idx_chat_logs_created_at ON chat_logs (created_at DESC);
