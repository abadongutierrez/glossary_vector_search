CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS vector_store (
    id uuid DEFAULT uuid_generate_v4() PRIMARY KEY,
    content text,
    metadata jsonb,
    embedding vector(768)
);

CREATE INDEX IF NOT EXISTS vector_store_embedding_idx 
    ON vector_store 
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);