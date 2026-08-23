package com.resumematcher.backend.profile;

import java.util.List;

import com.resumematcher.backend.dto.ResumeProfileResponse;
import com.resumematcher.backend.entity.Resume;
import com.resumematcher.backend.entity.ResumeProfile;
import com.resumematcher.backend.entity.ResumeSkill;
import com.resumematcher.backend.repository.ResumeProfileRepository;
import com.resumematcher.backend.repository.ResumeRepository;
import com.resumematcher.backend.repository.ResumeSkillRepository;
import com.resumematcher.backend.security.CurrentUserProvider;
import com.resumematcher.backend.security.ResumeAccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ResumeProfileQueryService {

	private final ResumeRepository resumeRepository;
	private final ResumeProfileRepository resumeProfileRepository;
	private final ResumeSkillRepository resumeSkillRepository;
	private final CurrentUserProvider currentUserProvider;

	public ResumeProfileQueryService(ResumeRepository resumeRepository, ResumeProfileRepository resumeProfileRepository,
			ResumeSkillRepository resumeSkillRepository, CurrentUserProvider currentUserProvider) {
		this.resumeRepository = resumeRepository;
		this.resumeProfileRepository = resumeProfileRepository;
		this.resumeSkillRepository = resumeSkillRepository;
		this.currentUserProvider = currentUserProvider;
	}

	public ResumeProfileResponse getProfile(String resumeId) {
		// ResumeRepository is used only for the ownership check here - the actual
		// profile lookup below is unchanged and still goes through
		// ResumeProfileRepository, exactly as before.
		Resume resume = resumeRepository.findById(resumeId)
				.orElseThrow(() -> new ResumeProfileNotFoundException(
						"No resume profile found for resume id: " + resumeId));

		String currentUserId = currentUserProvider.getCurrentUserId()
				.orElseThrow(() -> new ResumeAccessDeniedException("Not authorized to access this resume"));
		if (!currentUserId.equals(resume.getOwnerId())) {
			throw new ResumeAccessDeniedException("Not authorized to access this resume");
		}

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
