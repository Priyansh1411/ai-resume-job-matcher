package com.resumematcher.backend.profile;

import java.util.Set;

public record ExtractedProfile(
		String fullName,
		String email,
		String phone,
		Set<String> skills
) {
}
