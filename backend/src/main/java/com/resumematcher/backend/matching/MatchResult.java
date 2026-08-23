package com.resumematcher.backend.matching;

import java.util.Set;

public record MatchResult(
		int matchScorePercentage,
		Set<String> matchedSkills,
		Set<String> missingSkills,
		Set<String> matchedRequiredSkills,
		Set<String> missingRequiredSkills,
		Set<String> matchedPreferredSkills,
		Set<String> missingPreferredSkills
) {
}
