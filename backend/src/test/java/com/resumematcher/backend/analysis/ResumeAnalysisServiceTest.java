package com.resumematcher.backend.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.resumematcher.backend.dto.ResumeAnalysisResponse;
import com.resumematcher.backend.entity.ProcessingStatus;
import com.resumematcher.backend.entity.Resume;
import com.resumematcher.backend.matching.JobDescriptionRequirementParser;
import com.resumematcher.backend.matching.KeywordResumeJobMatcher;
import com.resumematcher.backend.matching.ResumeNotFoundException;
import com.resumematcher.backend.matching.ResumeNotReadyException;
import com.resumematcher.backend.profile.SkillDictionary;
import com.resumematcher.backend.profile.SkillKeywordMatcher;
import com.resumematcher.backend.repository.ResumeRepository;
import com.resumematcher.backend.repository.ResumeSkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResumeAnalysisServiceTest {

	private static final String JOB_DESCRIPTION =
			"We are looking for a backend engineer experienced with Java, Spring Boot, Docker, "
					+ "and MySQL to join our growing platform team and help build scalable APIs.";

	@Mock
	private ResumeRepository resumeRepository;

	@Mock
	private ResumeSkillRepository resumeSkillRepository;

	@Mock
	private OpenAiChatClient chatClient;

	private final KeywordResumeJobMatcher keywordResumeJobMatcher = new KeywordResumeJobMatcher(
			new JobDescriptionRequirementParser(new SkillKeywordMatcher(new SkillDictionary())));

	@Test
	void throwsAnalysisUnavailableExceptionWhenDisabled() {
		ResumeAnalysisService service = new ResumeAnalysisService(
				false, resumeRepository, resumeSkillRepository, keywordResumeJobMatcher, chatClient);

		assertThatThrownBy(() -> service.analyze("resume-1", JOB_DESCRIPTION))
				.isInstanceOf(AnalysisUnavailableException.class);
	}

	@Test
	void throwsResumeNotFoundExceptionWhenResumeMissing() {
		when(resumeRepository.findById("missing-id")).thenReturn(Optional.empty());

		ResumeAnalysisService service = new ResumeAnalysisService(
				true, resumeRepository, resumeSkillRepository, keywordResumeJobMatcher, chatClient);

		assertThatThrownBy(() -> service.analyze("missing-id", JOB_DESCRIPTION))
				.isInstanceOf(ResumeNotFoundException.class);
	}

	@Test
	void throwsResumeNotReadyExceptionWhenResumeStillProcessing() {
		Resume resume = new Resume();
		resume.setProcessingStatus(ProcessingStatus.PROCESSING);
		when(resumeRepository.findById("resume-1")).thenReturn(Optional.of(resume));

		ResumeAnalysisService service = new ResumeAnalysisService(
				true, resumeRepository, resumeSkillRepository, keywordResumeJobMatcher, chatClient);

		assertThatThrownBy(() -> service.analyze("resume-1", JOB_DESCRIPTION))
				.isInstanceOf(ResumeNotReadyException.class);
	}

	@Test
	void returnsTheParsedAnalysisOnSuccess() {
		Resume resume = new Resume();
		resume.setProcessingStatus(ProcessingStatus.COMPLETED);
		resume.setExtractedText("Skilled in Java and Docker.");
		when(resumeRepository.findById("resume-1")).thenReturn(Optional.of(resume));
		when(resumeSkillRepository.findByResumeId("resume-1")).thenReturn(List.of());
		when(chatClient.complete(anyString(), anyString())).thenReturn(
				"{\"strengths\":[\"Strong Java background\"],"
						+ "\"gaps\":[\"No AWS experience listed\"],"
						+ "\"suggestions\":[\"Highlight cloud experience if any\"]}");

		ResumeAnalysisService service = new ResumeAnalysisService(
				true, resumeRepository, resumeSkillRepository, keywordResumeJobMatcher, chatClient);

		ResumeAnalysisResponse result = service.analyze("resume-1", JOB_DESCRIPTION);

		assertThat(result.strengths()).containsExactly("Strong Java background");
		assertThat(result.gaps()).containsExactly("No AWS experience listed");
		assertThat(result.suggestions()).containsExactly("Highlight cloud experience if any");
	}

	@Test
	void throwsAnalysisUnavailableExceptionWhenTheModelResponseIsMalformed() {
		Resume resume = new Resume();
		resume.setProcessingStatus(ProcessingStatus.COMPLETED);
		resume.setExtractedText("Skilled in Java and Docker.");
		when(resumeRepository.findById("resume-1")).thenReturn(Optional.of(resume));
		when(resumeSkillRepository.findByResumeId("resume-1")).thenReturn(List.of());
		when(chatClient.complete(anyString(), anyString())).thenReturn("not json");

		ResumeAnalysisService service = new ResumeAnalysisService(
				true, resumeRepository, resumeSkillRepository, keywordResumeJobMatcher, chatClient);

		assertThatThrownBy(() -> service.analyze("resume-1", JOB_DESCRIPTION))
				.isInstanceOf(AnalysisUnavailableException.class);
	}

	@Test
	void throwsAnalysisUnavailableExceptionWhenTheModelResponseIsMissingAField() {
		Resume resume = new Resume();
		resume.setProcessingStatus(ProcessingStatus.COMPLETED);
		resume.setExtractedText("Skilled in Java and Docker.");
		when(resumeRepository.findById("resume-1")).thenReturn(Optional.of(resume));
		when(resumeSkillRepository.findByResumeId("resume-1")).thenReturn(List.of());
		when(chatClient.complete(anyString(), anyString())).thenReturn("{\"strengths\":[]}");

		ResumeAnalysisService service = new ResumeAnalysisService(
				true, resumeRepository, resumeSkillRepository, keywordResumeJobMatcher, chatClient);

		assertThatThrownBy(() -> service.analyze("resume-1", JOB_DESCRIPTION))
				.isInstanceOf(AnalysisUnavailableException.class);
	}

}
