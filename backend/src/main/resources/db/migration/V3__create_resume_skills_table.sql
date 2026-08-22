CREATE TABLE resume_skills (
    id CHAR(36) NOT NULL,
    resume_id CHAR(36) NOT NULL,
    skill_name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_resume_skills_resume_skill UNIQUE (resume_id, skill_name),
    CONSTRAINT fk_resume_skills_resume FOREIGN KEY (resume_id) REFERENCES resumes(id) ON DELETE CASCADE
) ENGINE=InnoDB;
