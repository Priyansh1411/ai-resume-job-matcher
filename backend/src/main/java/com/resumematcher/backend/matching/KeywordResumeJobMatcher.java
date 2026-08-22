package com.resumematcher.backend.matching;

import java.util.LinkedHashSet;
import java.util.Set;

import com.resumematcher.backend.profile.SkillKeywordMatcher;
import org.springframework.stereotype.Component;

@Component
public class KeywordResumeJobMatcher implements ResumeJobMatcher {

	private final SkillKeywordMatcher skillKeywordMatcher;

	public KeywordResumeJobMatcher(SkillKeywordMatcher skillKeywordMatcher) {
		this.skillKeywordMatcher = skillKeywordMatcher;
	}

	@Override
	public MatchResult match(Set<String> resumeSkills, String jobDescriptionText) {
		Set<String> jobDescriptionSkills = skillKeywordMatcher.findSkills(jobDescriptionText);

		if (jobDescriptionSkills.isEmpty()) {
			return new MatchResult(0, Set.of(), Set.of());
		}

		Set<String> matchedSkills = new LinkedHashSet<>(jobDescriptionSkills);
		matchedSkills.retainAll(resumeSkills);

		Set<String> missingSkills = new LinkedHashSet<>(jobDescriptionSkills);
		missingSkills.removeAll(resumeSkills);

		int matchScorePercentage = Math.round(100f * matchedSkills.size() / jobDescriptionSkills.size());

		return new MatchResult(matchScorePercentage, matchedSkills, missingSkills);
	}

}
