ALTER TABLE contents
    ADD COLUMN thumbnail_upload_status VARCHAR(20)
        CHECK (thumbnail_upload_status IN ('PENDING', 'COMPLETED', 'FAILED'));
