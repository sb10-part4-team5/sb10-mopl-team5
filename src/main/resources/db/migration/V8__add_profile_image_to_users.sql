ALTER TABLE users
    ADD COLUMN profile_image_id UUID
        CONSTRAINT fk_users_profile_image REFERENCES binary_contents (id) ON DELETE SET NULL;

ALTER TABLE users
    DROP COLUMN profile_image_url;
