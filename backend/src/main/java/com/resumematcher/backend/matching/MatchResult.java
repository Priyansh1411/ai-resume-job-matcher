package com.resumematcher.backend.matching;

import java.util.Set;

public record MatchResult(
		int matchScorePercentage,
		Set<String> matchedSkills,
		Set<String> missingSkills
) {
}
