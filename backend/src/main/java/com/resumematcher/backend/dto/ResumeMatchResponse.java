package com.resumematcher.backend.dto;

import java.util.Set;

public record ResumeMatchResponse(
		int matchScorePercentage,
		Set<String> matchedSkills,
		Set<String> missingSkills,
		Set<String> matchedRequiredSkills,
		Set<String> missingRequiredSkills,
		Set<String> matchedPreferredSkills,
		Set<String> missingPreferredSkills
) {
}
