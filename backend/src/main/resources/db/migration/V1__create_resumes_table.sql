CREATE TABLE resumes (
    id CHAR(36) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    extracted_text LONGTEXT NULL,
    processing_status VARCHAR(30) NOT NULL DEFAULT 'UPLOADED',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT chk_resumes_file_size_bytes_positive CHECK (file_size_bytes > 0)
) ENGINE=InnoDB;
