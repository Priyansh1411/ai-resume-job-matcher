package com.resumematcher.backend.profile;

import java.util.List;

import com.resumematcher.backend.dto.ResumeProfileResponse;
import com.resumematcher.backend.entity.ResumeProfile;
import com.resumematcher.backend.entity.ResumeSkill;
import com.resumematcher.backend.repository.ResumeProfileRepository;
import com.resumematcher.backend.repository.ResumeSkillRepository;
import org.springframework.stereotype.Service;

@Service
public class ResumeProfileQueryService {

	private final ResumeProfileRepository resumeProfileRepository;
	private final ResumeSkillRepository resumeSkillRepository;

	public ResumeProfileQueryService(ResumeProfileRepository resumeProfileRepository,
			ResumeSkillRepository resumeSkillRepository) {
		this.resumeProfileRepository = resumeProfileRepository;
		this.resumeSkillRepository = resumeSkillRepository;
	}

	public ResumeProfileResponse getProfile(String resumeId) {
		ResumeProfile profile = resumeProfileRepository.findByResumeId(resumeId)
				.orElseThrow(() -> new ResumeProfileNotFoundException(
						"No resume profile found for resume id: " + resumeId));

		List<String> skills = resumeSkillRepository.findByResumeId(resumeId).stream()
				.map(ResumeSkill::getSkillName)
				.toList();

		return new ResumeProfileResponse(
				profile.getFullName(),
				profile.getEmail(),
				profile.getPhone(),
				skills,
				profile.getProfileStatus()
		);
	}

}
