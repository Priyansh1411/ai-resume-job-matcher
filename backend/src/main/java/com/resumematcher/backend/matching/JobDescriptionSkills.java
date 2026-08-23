package com.resumematcher.backend.matching;

import java.util.Set;

public record JobDescriptionSkills(
		Set<String> requiredSkills,
		Set<String> preferredSkills
) {
}
