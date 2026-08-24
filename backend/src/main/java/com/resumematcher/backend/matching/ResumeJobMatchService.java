package com.resumematcher.backend.matching;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.resumematcher.backend.entity.ProcessingStatus;
import com.resumematcher.backend.entity.Resume;
import com.resumematcher.backend.entity.ResumeSkill;
import com.resumematcher.backend.observability.ResumeProcessingMetrics;
import com.resumematcher.backend.repository.ResumeRepository;
import com.resumematcher.backend.repository.ResumeSkillRepository;
import com.resumematcher.backend.security.CurrentUserProvider;
import com.resumematcher.backend.security.ResumeAccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ResumeJobMatchService {

	private final ResumeRepository resumeRepository;
	private final ResumeSkillRepository resumeSkillRepository;
	private final ResumeJobMatcher resumeJobMatcher;
	private final CurrentUserProvider currentUserProvider;
	private final ResumeProcessingMetrics resumeProcessingMetrics;

	public ResumeJobMatchService(ResumeRepository resumeRepository, ResumeSkillRepository resumeSkillRepository,
			ResumeJobMatcher resumeJobMatcher, CurrentUserProvider currentUserProvider,
			ResumeProcessingMetrics resumeProcessingMetrics) {
		this.resumeRepository = resumeRepository;
		this.resumeSkillRepository = resumeSkillRepository;
		this.resumeJobMatcher = resumeJobMatcher;
		this.currentUserProvider = currentUserProvider;
		this.resumeProcessingMetrics = resumeProcessingMetrics;
	}

	public MatchResult matchResumeToJobDescription(String resumeId, String jobDescriptionText) {
		Instant start = Instant.now();
		try {
			Resume resume = resumeRepository.findById(resumeId)
					.orElseThrow(() -> new ResumeNotFoundException("No resume found with id: " + resumeId));

			// Ownership before processing-status: a caller who doesn't own this resume
			// shouldn't be able to learn anything about its state, including whether
			// it's still processing.
			String currentUserId = currentUserProvider.getCurrentUserId()
					.orElseThrow(() -> new ResumeAccessDeniedException("Not authorized to access this resume"));
			if (!currentUserId.equals(resume.getOwnerId())) {
				throw new ResumeAccessDeniedException("Not authorized to access this resume");
			}

			if (resume.getProcessingStatus() != ProcessingStatus.COMPLETED) {
				throw new ResumeNotReadyException("Resume has not finished processing yet");
			}

			List<ResumeSkill> resumeSkills = resumeSkillRepository.findByResumeId(resumeId);
			Set<String> resumeSkillNames = resumeSkills.stream()
					.map(ResumeSkill::getSkillName)
					.collect(Collectors.toSet());

			return resumeJobMatcher.match(resumeSkillNames, resume.getExtractedText(), jobDescriptionText);
		} finally {
			resumeProcessingMetrics.recordMatchingDuration(Duration.between(start, Instant.now()));
		}
	}

}
