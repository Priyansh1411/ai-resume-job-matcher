package com.resumematcher.backend.matching;

import static org.assertj.core.api.Assertions.assertThat;

import com.resumematcher.backend.profile.SkillDictionary;
import com.resumematcher.backend.profile.SkillKeywordMatcher;
import org.junit.jupiter.api.Test;

class JobDescriptionRequirementParserTest {

	private final JobDescriptionRequirementParser parser =
			new JobDescriptionRequirementParser(new SkillKeywordMatcher(new SkillDictionary()));

	@Test
	void classifiesSkillsUnderRequiredAndPreferredHeaders() {
		String jobDescriptionText = """
				Required:
				Java, Docker

				Preferred:
				Kubernetes, AWS
				""";

		JobDescriptionSkills skills = parser.parse(jobDescriptionText);

		assertThat(skills.requiredSkills()).containsExactlyInAnyOrder("java", "docker");
		assertThat(skills.preferredSkills()).containsExactlyInAnyOrder("kubernetes", "aws");
	}

	@Test
	void recognizesVariousRequiredAndPreferredHeaderPhrasings() {
		String jobDescriptionText = """
				Must Have:
				Python

				Nice to Have:
				React
				""";

		JobDescriptionSkills skills = parser.parse(jobDescriptionText);

		assertThat(skills.requiredSkills()).containsExactly("python");
		assertThat(skills.preferredSkills()).containsExactly("react");
	}

	@Test
	void defaultsEverythingToRequiredWhenNoHeadersArePresent() {
		String jobDescriptionText = "Looking for someone skilled in Java, Docker, Kubernetes and AWS.";

		JobDescriptionSkills skills = parser.parse(jobDescriptionText);

		assertThat(skills.requiredSkills()).containsExactlyInAnyOrder("java", "docker", "kubernetes", "aws");
		assertThat(skills.preferredSkills()).isEmpty();
	}

	@Test
	void treatsASkillMentionedInBothSectionsAsRequiredOnly() {
		String jobDescriptionText = """
				Required:
				Java

				Preferred:
				Java, Docker
				""";

		JobDescriptionSkills skills = parser.parse(jobDescriptionText);

		assertThat(skills.requiredSkills()).containsExactly("java");
		assertThat(skills.preferredSkills()).containsExactly("docker");
	}

	@Test
	void returnsEmptySetsForBlankOrNullInput() {
		assertThat(parser.parse("").requiredSkills()).isEmpty();
		assertThat(parser.parse("").preferredSkills()).isEmpty();
		assertThat(parser.parse(null).requiredSkills()).isEmpty();
		assertThat(parser.parse(null).preferredSkills()).isEmpty();
	}

}
