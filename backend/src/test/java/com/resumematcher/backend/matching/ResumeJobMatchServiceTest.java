package com.resumematcher.backend.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.resumematcher.backend.entity.ProcessingStatus;
import com.resumematcher.backend.entity.Resume;
import com.resumematcher.backend.entity.ResumeSkill;
import com.resumematcher.backend.repository.ResumeRepository;
import com.resumematcher.backend.repository.ResumeSkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeJobMatchServiceTest {

	@Mock
	private ResumeRepository resumeRepository;

	@Mock
	private ResumeSkillRepository resumeSkillRepository;

	@Mock
	private ResumeJobMatcher resumeJobMatcher;

	@Test
	void delegatesToMatcherWithResumeSkillsWhenResumeIsCompleted() {
		Resume resume = new Resume();
		resume.setProcessingStatus(ProcessingStatus.COMPLETED);

		when(resumeRepository.findById("resume-1")).thenReturn(Optional.of(resume));
		when(resumeSkillRepository.findByResumeId("resume-1")).thenReturn(List.of(
				new ResumeSkill("resume-1", "java"),
				new ResumeSkill("resume-1", "docker")));
		when(resumeJobMatcher.match(eq(Set.of("java", "docker")), eq("job text")))
				.thenReturn(new MatchResult(80, Set.of("java"), Set.of("aws"),
						Set.of("java"), Set.of("aws"), Set.of(), Set.of()));

		ResumeJobMatchService service =
				new ResumeJobMatchService(resumeRepository, resumeSkillRepository, resumeJobMatcher);

		MatchResult result = service.matchResumeToJobDescription("resume-1", "job text");

		assertThat(result.matchScorePercentage()).isEqualTo(80);
		assertThat(result.matchedSkills()).containsExactly("java");
		assertThat(result.missingSkills()).containsExactly("aws");
		assertThat(result.matchedRequiredSkills()).containsExactly("java");
		assertThat(result.missingRequiredSkills()).containsExactly("aws");
		assertThat(result.matchedPreferredSkills()).isEmpty();
		assertThat(result.missingPreferredSkills()).isEmpty();
	}

	@Test
	void throwsNotFoundWhenResumeDoesNotExist() {
		when(resumeRepository.findById("missing-id")).thenReturn(Optional.empty());

		ResumeJobMatchService service =
				new ResumeJobMatchService(resumeRepository, resumeSkillRepository, resumeJobMatcher);

		assertThatThrownBy(() -> service.matchResumeToJobDescription("missing-id", "job text"))
				.isInstanceOf(ResumeNotFoundException.class);
	}

	@Test
	void throwsNotReadyWhenResumeProcessingIsNotCompleted() {
		Resume resume = new Resume();
		resume.setProcessingStatus(ProcessingStatus.PROCESSING);

		when(resumeRepository.findById("resume-2")).thenReturn(Optional.of(resume));

		ResumeJobMatchService service =
				new ResumeJobMatchService(resumeRepository, resumeSkillRepository, resumeJobMatcher);

		assertThatThrownBy(() -> service.matchResumeToJobDescription("resume-2", "job text"))
				.isInstanceOf(ResumeNotReadyException.class);
	}

}
