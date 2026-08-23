package com.resumematcher.backend.dto;

import java.util.List;

public record ResumeAnalysisResponse(
		List<String> strengths,
		List<String> gaps,
		List<String> suggestions
) {
}
