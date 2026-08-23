ALTER TABLE resumes
    ADD COLUMN owner_id CHAR(36) NULL,
    ADD CONSTRAINT fk_resumes_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL;
