package com.resumematcher.backend.repository;

import java.util.Optional;

import com.resumematcher.backend.entity.ResumeProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeProfileRepository extends JpaRepository<ResumeProfile, String> {

	Optional<ResumeProfile> findByResumeId(String resumeId);

}
