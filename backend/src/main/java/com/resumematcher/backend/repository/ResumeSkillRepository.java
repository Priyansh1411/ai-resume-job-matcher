package com.resumematcher.backend.repository;

import java.util.List;

import com.resumematcher.backend.entity.ResumeSkill;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeSkillRepository extends JpaRepository<ResumeSkill, String> {

	List<ResumeSkill> findByResumeId(String resumeId);

}
