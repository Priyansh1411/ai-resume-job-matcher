package com.resumematcher.backend.matching;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.resumematcher.backend.entity.ProcessingStatus;
import com.resumematcher.backend.entity.Resume;
import com.resumematcher.backend.entity.ResumeSkill;
import com.resumematcher.backend.repository.ResumeRepository;
import com.resumematcher.backend.repository.ResumeSkillRepository;
import org.springframework.stereotype.Service;

@Service
public class ResumeJobMatchService {

	private final ResumeRepository resumeRepository;
	private final ResumeSkillRepository resumeSkillRepository;
	private final ResumeJobMatcher resumeJobMatcher;

	public ResumeJobMatchService(ResumeRepository resumeRepository, ResumeSkillRepository resumeSkillRepository,
			ResumeJobMatcher resumeJobMatcher) {
		this.resumeRepository = resumeRepository;
		this.resumeSkillRepository = resumeSkillRepository;
		this.resumeJobMatcher = resumeJobMatcher;
	}

	public MatchResult matchResumeToJobDescription(String resumeId, String jobDescriptionText) {
		Resume resume = resumeRepository.findById(resumeId)
				.orElseThrow(() -> new ResumeNotFoundException("No resume found with id: " + resumeId));

		if (resume.getProcessingStatus() != ProcessingStatus.COMPLETED) {
			throw new ResumeNotReadyException("Resume has not finished processing yet");
		}

		List<ResumeSkill> resumeSkills = resumeSkillRepository.findByResumeId(resumeId);
		Set<String> resumeSkillNames = resumeSkills.stream()
				.map(ResumeSkill::getSkillName)
				.collect(Collectors.toSet());

		return resumeJobMatcher.match(resumeSkillNames, jobDescriptionText);
	}

}
