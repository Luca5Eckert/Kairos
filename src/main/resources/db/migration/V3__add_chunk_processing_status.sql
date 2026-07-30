ALTER TABLE chunks
    ADD COLUMN processing_status TEXT;

UPDATE chunks
SET processing_status = CASE
    WHEN processed THEN 'COMPLETED'
    ELSE 'PENDING'
END;

ALTER TABLE chunks
    ALTER COLUMN processing_status SET NOT NULL,
    ALTER COLUMN processing_status SET DEFAULT 'PENDING',
    ADD CONSTRAINT ck_chunks_processing_status
        CHECK (processing_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'));

CREATE INDEX idx_chunks_source_processing_status
    ON chunks(source_id, processing_status);

ALTER TABLE chunks
    DROP COLUMN processed;
