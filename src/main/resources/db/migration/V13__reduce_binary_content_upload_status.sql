UPDATE binary_contents
SET upload_status = 'COMPLETED'
WHERE upload_status IN ('PENDING', 'FAILED');

ALTER TABLE binary_contents
    DROP CONSTRAINT ck_binary_contents_upload_status;

ALTER TABLE binary_contents
    ADD CONSTRAINT ck_binary_contents_upload_status
        CHECK (upload_status IN ('COMPLETED', 'DELETED'));
