package com.resumematcher.backend.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.resumematcher.backend.dto.ResumeProfileResponse;
import com.resumematcher.backend.entity.ProfileStatus;
import com.resumematcher.backend.entity.ResumeProfile;
import com.resumematcher.backend.entity.ResumeSkill;
import com.resumematcher.backend.repository.ResumeProfileRepository;
import com.resumematcher.backend.repository.ResumeSkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeProfileQueryServiceTest {

	@Mock
	private ResumeProfileRepository resumeProfileRepository;

	@Mock
	private ResumeSkillRepository resumeSkillRepository;

	@Test
	void returnsProfileWithSkillsWhenFound() {
		ResumeProfile profile = new ResumeProfile();
		profile.setFullName("Jane Doe");
		profile.setEmail("jane@example.com");
		profile.setPhone("555-1234");
		profile.setProfileStatus(ProfileStatus.COMPLETED);

		when(resumeProfileRepository.findByResumeId("resume-1")).thenReturn(Optional.of(profile));
		when(resumeSkillRepository.findByResumeId("resume-1")).thenReturn(List.of(
				new ResumeSkill("resume-1", "java"),
				new ResumeSkill("resume-1", "docker")));

		ResumeProfileQueryService service =
				new ResumeProfileQueryService(resumeProfileRepository, resumeSkillRepository);

		ResumeProfileResponse response = service.getProfile("resume-1");

		assertThat(response.fullName()).isEqualTo("Jane Doe");
		assertThat(response.email()).isEqualTo("jane@example.com");
		assertThat(response.phone()).isEqualTo("555-1234");
		assertThat(response.skills()).containsExactlyInAnyOrder("java", "docker");
		assertThat(response.profileStatus()).isEqualTo(ProfileStatus.COMPLETED);
	}

	@Test
	void throwsNotFoundWhenNoProfileExistsForResumeId() {
		when(resumeProfileRepository.findByResumeId("missing-id")).thenReturn(Optional.empty());

		ResumeProfileQueryService service =
				new ResumeProfileQueryService(resumeProfileRepository, resumeSkillRepository);

		assertThatThrownBy(() -> service.getProfile("missing-id"))
				.isInstanceOf(ResumeProfileNotFoundException.class);
	}

}
