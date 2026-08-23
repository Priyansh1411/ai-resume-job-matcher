package com.resumematcher.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalysisRequest(
		@NotBlank(message = "Job description must not be blank")
		@Size(min = 100, message = "Job description must be at least 100 characters")
		String jobDescription
) {
}
