package com.resumematcher.backend.profile;

import com.resumematcher.backend.entity.ProfileStatus;
import com.resumematcher.backend.entity.ResumeProfile;
import com.resumematcher.backend.entity.ResumeSkill;
import com.resumematcher.backend.repository.ResumeProfileRepository;
import com.resumematcher.backend.repository.ResumeSkillRepository;
import org.springframework.stereotype.Service;

@Service
public class ResumeProfileExtractionService {

	private final ResumeProfileExtractor resumeProfileExtractor;
	private final ResumeProfileRepository resumeProfileRepository;
	private final ResumeSkillRepository resumeSkillRepository;

	public ResumeProfileExtractionService(ResumeProfileExtractor resumeProfileExtractor,
			ResumeProfileRepository resumeProfileRepository, ResumeSkillRepository resumeSkillRepository) {
		this.resumeProfileExtractor = resumeProfileExtractor;
		this.resumeProfileRepository = resumeProfileRepository;
		this.resumeSkillRepository = resumeSkillRepository;
	}

	public void extractAndStoreProfile(String resumeId, String resumeText) {
		ResumeProfile profile = new ResumeProfile();
		profile.setResumeId(resumeId);
		profile.setProfileStatus(ProfileStatus.PROCESSING);
		resumeProfileRepository.save(profile);

		try {
			ExtractedProfile extractedProfile = resumeProfileExtractor.extract(resumeText);

			profile.setFullName(extractedProfile.fullName());
			profile.setEmail(extractedProfile.email());
			profile.setPhone(extractedProfile.phone());
			profile.setProfileStatus(ProfileStatus.COMPLETED);

			extractedProfile.skills().forEach(skill ->
					resumeSkillRepository.save(new ResumeSkill(resumeId, skill)));
		} catch (Exception e) {
			profile.setFullName(null);
			profile.setEmail(null);
			profile.setPhone(null);
			profile.setProfileStatus(ProfileStatus.FAILED);
		}

		resumeProfileRepository.save(profile);
	}

}
