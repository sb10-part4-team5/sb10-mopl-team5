ALTER TABLE contents
    DROP CONSTRAINT ck_contents_source;

ALTER TABLE contents
    ADD CONSTRAINT ck_contents_source CHECK (source IN ('TMDB', 'SPORTS_DB', 'ADMIN'));
