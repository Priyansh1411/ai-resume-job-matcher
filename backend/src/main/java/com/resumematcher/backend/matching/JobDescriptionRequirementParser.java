package com.resumematcher.backend.matching;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import com.resumematcher.backend.profile.SkillKeywordMatcher;
import org.springframework.stereotype.Component;

@Component
public class JobDescriptionRequirementParser {

	private static final Pattern REQUIRED_HEADER_PATTERN = Pattern.compile(
			"^(requirements?|required qualifications?|required skills?|must[-\\s]haves?|"
					+ "minimum qualifications?|basic qualifications?)\\s*:?\\s*$",
			Pattern.CASE_INSENSITIVE);

	private static final Pattern PREFERRED_HEADER_PATTERN = Pattern.compile(
			"^(preferred(?: qualifications?| skills?)?|nice[-\\s]to[-\\s]haves?|bonus(?: points?)?|"
					+ "good[-\\s]to[-\\s]haves?)\\s*:?\\s*$",
			Pattern.CASE_INSENSITIVE);

	private enum Section {
		REQUIRED,
		PREFERRED
	}

	private final SkillKeywordMatcher skillKeywordMatcher;

	public JobDescriptionRequirementParser(SkillKeywordMatcher skillKeywordMatcher) {
		this.skillKeywordMatcher = skillKeywordMatcher;
	}

	public JobDescriptionSkills parse(String jobDescriptionText) {
		if (jobDescriptionText == null || jobDescriptionText.isBlank()) {
			return new JobDescriptionSkills(Set.of(), Set.of());
		}

		StringBuilder requiredText = new StringBuilder();
		StringBuilder preferredText = new StringBuilder();
		Section currentSection = Section.REQUIRED;

		for (String line : jobDescriptionText.split("\\R")) {
			String trimmedLine = line.trim();

			if (REQUIRED_HEADER_PATTERN.matcher(trimmedLine).matches()) {
				currentSection = Section.REQUIRED;
				continue;
			}
			if (PREFERRED_HEADER_PATTERN.matcher(trimmedLine).matches()) {
				currentSection = Section.PREFERRED;
				continue;
			}

			if (currentSection == Section.REQUIRED) {
				requiredText.append(line).append('\n');
			} else {
				preferredText.append(line).append('\n');
			}
		}

		Set<String> requiredSkills = new LinkedHashSet<>(skillKeywordMatcher.findSkills(requiredText.toString()));
		Set<String> preferredSkills = new LinkedHashSet<>(skillKeywordMatcher.findSkills(preferredText.toString()));

		// A skill mentioned in both sections is treated as required (the stronger signal wins).
		preferredSkills.removeAll(requiredSkills);

		return new JobDescriptionSkills(requiredSkills, preferredSkills);
	}

}
