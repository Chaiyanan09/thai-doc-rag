-- pgvector: adds the vector type and distance operators (<=> cosine, <-> L2)
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE documents (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    filename      text        NOT NULL,
    status        varchar(20) NOT NULL CHECK (status IN ('PROCESSING', 'READY', 'FAILED')),
    page_count    integer     NOT NULL DEFAULT 0,
    chunk_count   integer     NOT NULL DEFAULT 0,
    empty_pages   integer[]   NOT NULL DEFAULT '{}',  -- pages with almost no text (likely scanned)
    error_message text,
    created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE chunks (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    document_id    uuid         NOT NULL REFERENCES documents (id) ON DELETE CASCADE,
    chunk_index    integer      NOT NULL,
    page_number    integer      NOT NULL,
    content        text         NOT NULL,
    content_tokens text         NOT NULL,          -- Thai words split by PyThaiNLP, for keyword search (week 3)
    embedding      vector(1024) NOT NULL,          -- bge-m3, normalized to length 1
    created_at     timestamptz  NOT NULL DEFAULT now(),
    UNIQUE (document_id, chunk_index)              -- also serves as the index for document_id lookups
);

-- Approximate nearest-neighbour index for cosine distance (embedding <=> query)
CREATE INDEX idx_chunks_embedding ON chunks USING hnsw (embedding vector_cosine_ops);
