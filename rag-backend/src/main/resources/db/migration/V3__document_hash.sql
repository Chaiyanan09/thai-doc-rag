-- SHA-256 of the uploaded PDF bytes. Attaching the same file again reuses the READY document
-- instead of re-reading and re-embedding it (rows uploaded before V3 stay NULL).
ALTER TABLE documents ADD COLUMN content_hash varchar(64);

CREATE INDEX idx_documents_content_hash ON documents (content_hash);
