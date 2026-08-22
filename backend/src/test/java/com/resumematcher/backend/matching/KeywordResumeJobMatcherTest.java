package com.resumematcher.backend.matching;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import com.resumematcher.backend.profile.SkillKeywordMatcher;
import org.junit.jupiter.api.Test;

class KeywordResumeJobMatcherTest {

	private final KeywordResumeJobMatcher matcher = new KeywordResumeJobMatcher(new SkillKeywordMatcher());

	@Test
	void computesScoreAndMatchedMissingSkills() {
		Set<String> resumeSkills = Set.of("java", "docker", "mysql");
		String jobDescriptionText = "Looking for someone skilled in Java, Docker, Kubernetes and AWS.";

		MatchResult result = matcher.match(resumeSkills, jobDescriptionText);

		assertThat(result.matchedSkills()).containsExactlyInAnyOrder("java", "docker");
		assertThat(result.missingSkills()).containsExactlyInAnyOrder("kubernetes", "aws");
		assertThat(result.matchScorePercentage()).isEqualTo(50);
	}

	@Test
	void returnsFullScoreWhenAllRequiredSkillsPresent() {
		Set<String> resumeSkills = Set.of("java", "react");
		String jobDescriptionText = "We need a developer who knows Java and React.";

		MatchResult result = matcher.match(resumeSkills, jobDescriptionText);

		assertThat(result.matchScorePercentage()).isEqualTo(100);
		assertThat(result.missingSkills()).isEmpty();
	}

	@Test
	void returnsZeroScoreWhenJobDescriptionHasNoRecognizedSkills() {
		Set<String> resumeSkills = Set.of("java", "docker");
		String jobDescriptionText = "This posting mentions nothing recognizable at all.";

		MatchResult result = matcher.match(resumeSkills, jobDescriptionText);

		assertThat(result.matchScorePercentage()).isZero();
		assertThat(result.matchedSkills()).isEmpty();
		assertThat(result.missingSkills()).isEmpty();
	}

}
