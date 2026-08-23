package com.resumematcher.backend.matching;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class KeywordResumeJobMatcher implements ResumeJobMatcher {

	private static final int REQUIRED_WEIGHT = 2;
	private static final int PREFERRED_WEIGHT = 1;

	private final JobDescriptionRequirementParser jobDescriptionRequirementParser;

	public KeywordResumeJobMatcher(JobDescriptionRequirementParser jobDescriptionRequirementParser) {
		this.jobDescriptionRequirementParser = jobDescriptionRequirementParser;
	}

	@Override
	public MatchResult match(Set<String> resumeSkills, String jobDescriptionText) {
		JobDescriptionSkills jobDescriptionSkills = jobDescriptionRequirementParser.parse(jobDescriptionText);
		Set<String> requiredSkills = jobDescriptionSkills.requiredSkills();
		Set<String> preferredSkills = jobDescriptionSkills.preferredSkills();

		if (requiredSkills.isEmpty() && preferredSkills.isEmpty()) {
			return new MatchResult(0, Set.of(), Set.of(), Set.of(), Set.of(), Set.of(), Set.of());
		}

		Set<String> matchedRequired = intersect(requiredSkills, resumeSkills);
		Set<String> missingRequired = difference(requiredSkills, resumeSkills);
		Set<String> matchedPreferred = intersect(preferredSkills, resumeSkills);
		Set<String> missingPreferred = difference(preferredSkills, resumeSkills);

		int weightedMatched = matchedRequired.size() * REQUIRED_WEIGHT + matchedPreferred.size() * PREFERRED_WEIGHT;
		int weightedTotal = requiredSkills.size() * REQUIRED_WEIGHT + preferredSkills.size() * PREFERRED_WEIGHT;
		int matchScorePercentage = Math.round(100f * weightedMatched / weightedTotal);

		Set<String> matchedSkills = union(matchedRequired, matchedPreferred);
		Set<String> missingSkills = union(missingRequired, missingPreferred);

		return new MatchResult(matchScorePercentage, matchedSkills, missingSkills,
				matchedRequired, missingRequired, matchedPreferred, missingPreferred);
	}

	private Set<String> intersect(Set<String> a, Set<String> b) {
		Set<String> result = new LinkedHashSet<>(a);
		result.retainAll(b);
		return result;
	}

	private Set<String> difference(Set<String> a, Set<String> b) {
		Set<String> result = new LinkedHashSet<>(a);
		result.removeAll(b);
		return result;
	}

	private Set<String> union(Set<String> a, Set<String> b) {
		Set<String> result = new LinkedHashSet<>(a);
		result.addAll(b);
		return result;
	}

}
