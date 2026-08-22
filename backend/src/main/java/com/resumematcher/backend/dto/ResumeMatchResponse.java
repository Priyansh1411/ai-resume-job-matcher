package com.resumematcher.backend.dto;

import java.util.Set;

public record ResumeMatchResponse(
		int matchScorePercentage,
		Set<String> matchedSkills,
		Set<String> missingSkills
) {
}
