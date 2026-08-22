package com.resumematcher.backend.dto;

import java.util.List;

import com.resumematcher.backend.entity.ProfileStatus;

public record ResumeProfileResponse(
		String fullName,
		String email,
		String phone,
		List<String> skills,
		ProfileStatus profileStatus
) {
}
