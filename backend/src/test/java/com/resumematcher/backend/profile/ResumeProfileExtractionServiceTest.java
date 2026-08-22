package com.resumematcher.backend.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import com.resumematcher.backend.entity.ProfileStatus;
import com.resumematcher.backend.entity.ResumeProfile;
import com.resumematcher.backend.entity.ResumeSkill;
import com.resumematcher.backend.repository.ResumeProfileRepository;
import com.resumematcher.backend.repository.ResumeSkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeProfileExtractionServiceTest {

	@Mock
	private ResumeProfileExtractor resumeProfileExtractor;

	@Mock
	private ResumeProfileRepository resumeProfileRepository;

	@Mock
	private ResumeSkillRepository resumeSkillRepository;

	@Captor
	private ArgumentCaptor<ResumeProfile> profileCaptor;

	@Test
	void savesCompletedProfileAndSkillsOnSuccessfulExtraction() {
		when(resumeProfileExtractor.extract("resume text")).thenReturn(
				new ExtractedProfile("Jane Doe", "jane@example.com", "555-1234", Set.of("java", "docker")));
		when(resumeProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		ResumeProfileExtractionService service = new ResumeProfileExtractionService(
				resumeProfileExtractor, resumeProfileRepository, resumeSkillRepository);

		service.extractAndStoreProfile("resume-1", "resume text");

		verify(resumeProfileRepository, times(2)).save(profileCaptor.capture());
		ResumeProfile finalSave = profileCaptor.getAllValues().get(1);
		assertThat(finalSave.getProfileStatus()).isEqualTo(ProfileStatus.COMPLETED);
		assertThat(finalSave.getFullName()).isEqualTo("Jane Doe");
		assertThat(finalSave.getEmail()).isEqualTo("jane@example.com");
		assertThat(finalSave.getResumeId()).isEqualTo("resume-1");

		verify(resumeSkillRepository, times(2)).save(any(ResumeSkill.class));
	}

	@Test
	void savesFailedProfileWithoutSkillsWhenExtractionThrows() {
		when(resumeProfileExtractor.extract(any())).thenThrow(new RuntimeException("boom"));
		when(resumeProfileRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		ResumeProfileExtractionService service = new ResumeProfileExtractionService(
				resumeProfileExtractor, resumeProfileRepository, resumeSkillRepository);

		service.extractAndStoreProfile("resume-2", "resume text");

		verify(resumeProfileRepository, times(2)).save(profileCaptor.capture());
		ResumeProfile finalSave = profileCaptor.getAllValues().get(1);
		assertThat(finalSave.getProfileStatus()).isEqualTo(ProfileStatus.FAILED);
		assertThat(finalSave.getFullName()).isNull();
		assertThat(finalSave.getEmail()).isNull();

		verify(resumeSkillRepository, times(0)).save(any());
	}

}
