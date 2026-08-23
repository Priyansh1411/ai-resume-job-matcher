package com.resumematcher.backend.profile;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RuleBasedResumeProfileExtractorTest {

	private final RuleBasedResumeProfileExtractor extractor =
			new RuleBasedResumeProfileExtractor(new SkillKeywordMatcher(new SkillDictionary()));

	@Test
	void extractsNameEmailPhoneAndSkillsFromResumeText() {
		String resumeText = """
				Jane Doe
				Email: jane.doe@example.com
				Phone: (415) 555-2671

				Experienced backend engineer skilled in Java, Spring Boot, Docker and MySQL.
				""";

		ExtractedProfile profile = extractor.extract(resumeText);

		assertThat(profile.fullName()).isEqualTo("Jane Doe");
		assertThat(profile.email()).isEqualTo("jane.doe@example.com");
		assertThat(profile.phone()).contains("415").contains("555").contains("2671");
		assertThat(profile.skills()).contains("java", "spring boot", "docker", "mysql");
	}

	@Test
	void returnsNullContactFieldsWhenNotPresent() {
		String resumeText = "General experience with problem solving and teamwork.";

		ExtractedProfile profile = extractor.extract(resumeText);

		assertThat(profile.email()).isNull();
		assertThat(profile.phone()).isNull();
		assertThat(profile.skills()).contains("problem solving", "teamwork");
	}

	@Test
	void returnsEmptySkillsWhenNoKnownSkillsPresent() {
		String resumeText = "This document mentions nothing recognizable at all.";

		ExtractedProfile profile = extractor.extract(resumeText);

		assertThat(profile.skills()).isEmpty();
	}

}
