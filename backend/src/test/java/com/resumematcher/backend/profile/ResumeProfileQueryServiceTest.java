package com.resumematcher.backend.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.resumematcher.backend.dto.ResumeProfileResponse;
import com.resumematcher.backend.entity.ProfileStatus;
import com.resumematcher.backend.entity.Resume;
import com.resumematcher.backend.entity.ResumeProfile;
import com.resumematcher.backend.entity.ResumeSkill;
import com.resumematcher.backend.repository.ResumeProfileRepository;
import com.resumematcher.backend.repository.ResumeRepository;
import com.resumematcher.backend.repository.ResumeSkillRepository;
import com.resumematcher.backend.security.CurrentUserProvider;
import com.resumematcher.backend.security.ResumeAccessDeniedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeProfileQueryServiceTest {

	@Mock
	private ResumeRepository resumeRepository;

	@Mock
	private ResumeProfileRepository resumeProfileRepository;

	@Mock
	private ResumeSkillRepository resumeSkillRepository;

	@Mock
	private CurrentUserProvider currentUserProvider;

	@Test
	void returnsProfileWithSkillsWhenFound() {
		Resume resume = new Resume();
		resume.setOwnerId("user-1");
		when(resumeRepository.findById("resume-1")).thenReturn(Optional.of(resume));
		when(currentUserProvider.getCurrentUserId()).thenReturn(Optional.of("user-1"));

		ResumeProfile profile = new ResumeProfile();
		profile.setFullName("Jane Doe");
		profile.setEmail("jane@example.com");
		profile.setPhone("555-1234");
		profile.setProfileStatus(ProfileStatus.COMPLETED);

		when(resumeProfileRepository.findByResumeId("resume-1")).thenReturn(Optional.of(profile));
		when(resumeSkillRepository.findByResumeId("resume-1")).thenReturn(List.of(
				new ResumeSkill("resume-1", "java"),
				new ResumeSkill("resume-1", "docker")));

		ResumeProfileQueryService service = new ResumeProfileQueryService(
				resumeRepository, resumeProfileRepository, resumeSkillRepository, currentUserProvider);

		ResumeProfileResponse response = service.getProfile("resume-1");

		assertThat(response.fullName()).isEqualTo("Jane Doe");
		assertThat(response.email()).isEqualTo("jane@example.com");
		assertThat(response.phone()).isEqualTo("555-1234");
		assertThat(response.skills()).containsExactlyInAnyOrder("java", "docker");
		assertThat(response.profileStatus()).isEqualTo(ProfileStatus.COMPLETED);
	}

	@Test
	void throwsNotFoundWhenNoResumeExistsForResumeId() {
		when(resumeRepository.findById("missing-id")).thenReturn(Optional.empty());

		ResumeProfileQueryService service = new ResumeProfileQueryService(
				resumeRepository, resumeProfileRepository, resumeSkillRepository, currentUserProvider);

		assertThatThrownBy(() -> service.getProfile("missing-id"))
				.isInstanceOf(ResumeProfileNotFoundException.class);
	}

	@Test
	void throwsNotFoundWhenResumeExistsButHasNoProfileYet() {
		Resume resume = new Resume();
		resume.setOwnerId("user-1");
		when(resumeRepository.findById("resume-1")).thenReturn(Optional.of(resume));
		when(currentUserProvider.getCurrentUserId()).thenReturn(Optional.of("user-1"));
		when(resumeProfileRepository.findByResumeId("resume-1")).thenReturn(Optional.empty());

		ResumeProfileQueryService service = new ResumeProfileQueryService(
				resumeRepository, resumeProfileRepository, resumeSkillRepository, currentUserProvider);

		assertThatThrownBy(() -> service.getProfile("resume-1"))
				.isInstanceOf(ResumeProfileNotFoundException.class);
	}

	@Test
	void throwsAccessDeniedWhenTheCurrentUserDoesNotOwnTheResume() {
		Resume resume = new Resume();
		resume.setOwnerId("user-1");
		when(resumeRepository.findById("resume-1")).thenReturn(Optional.of(resume));
		when(currentUserProvider.getCurrentUserId()).thenReturn(Optional.of("user-2"));

		ResumeProfileQueryService service = new ResumeProfileQueryService(
				resumeRepository, resumeProfileRepository, resumeSkillRepository, currentUserProvider);

		assertThatThrownBy(() -> service.getProfile("resume-1"))
				.isInstanceOf(ResumeAccessDeniedException.class);
	}

}
