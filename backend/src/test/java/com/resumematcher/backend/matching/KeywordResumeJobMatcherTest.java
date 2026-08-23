package com.resumematcher.backend.matching;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import com.resumematcher.backend.profile.SkillDictionary;
import com.resumematcher.backend.profile.SkillKeywordMatcher;
import org.junit.jupiter.api.Test;

class KeywordResumeJobMatcherTest {

	private final KeywordResumeJobMatcher matcher = new KeywordResumeJobMatcher(
			new JobDescriptionRequirementParser(new SkillKeywordMatcher(new SkillDictionary())));

	@Test
	void computesScoreAndMatchedMissingSkills() {
		Set<String> resumeSkills = Set.of("java", "docker", "mysql");
		String jobDescriptionText = "Looking for someone skilled in Java, Docker, Kubernetes and AWS.";

		MatchResult result = matcher.match(resumeSkills, "irrelevant resume text", jobDescriptionText);

		assertThat(result.matchedSkills()).containsExactlyInAnyOrder("java", "docker");
		assertThat(result.missingSkills()).containsExactlyInAnyOrder("kubernetes", "aws");
		assertThat(result.matchScorePercentage()).isEqualTo(50);
	}

	@Test
	void returnsFullScoreWhenAllRequiredSkillsPresent() {
		Set<String> resumeSkills = Set.of("java", "react");
		String jobDescriptionText = "We need a developer who knows Java and React.";

		MatchResult result = matcher.match(resumeSkills, "irrelevant resume text", jobDescriptionText);

		assertThat(result.matchScorePercentage()).isEqualTo(100);
		assertThat(result.missingSkills()).isEmpty();
	}

	@Test
	void returnsZeroScoreWhenJobDescriptionHasNoRecognizedSkills() {
		Set<String> resumeSkills = Set.of("java", "docker");
		String jobDescriptionText = "This posting mentions nothing recognizable at all.";

		MatchResult result = matcher.match(resumeSkills, "irrelevant resume text", jobDescriptionText);

		assertThat(result.matchScorePercentage()).isZero();
		assertThat(result.matchedSkills()).isEmpty();
		assertThat(result.missingSkills()).isEmpty();
	}

	@Test
	void weighsAMatchedRequiredSkillMoreThanAMatchedPreferredSkillWouldHaveScoredUnderFlatCoverage() {
		Set<String> resumeSkills = Set.of("java");
		String jobDescriptionText = """
				Required:
				Java, Docker

				Preferred:
				Kubernetes, AWS
				""";

		MatchResult result = matcher.match(resumeSkills, "irrelevant resume text", jobDescriptionText);

		// Flat coverage would be 1 matched / 4 total = 25%. Weighting the matched
		// required skill (java) at 2x pulls the score above flat coverage.
		assertThat(result.matchScorePercentage()).isEqualTo(33);
		assertThat(result.matchedSkills()).containsExactly("java");
		assertThat(result.missingSkills()).containsExactlyInAnyOrder("docker", "kubernetes", "aws");

		assertThat(result.matchedRequiredSkills()).containsExactly("java");
		assertThat(result.missingRequiredSkills()).containsExactly("docker");
		assertThat(result.matchedPreferredSkills()).isEmpty();
		assertThat(result.missingPreferredSkills()).containsExactlyInAnyOrder("kubernetes", "aws");
	}

	@Test
	void weighsAMatchedPreferredSkillLessThanFlatCoverageWouldHaveScored() {
		Set<String> resumeSkills = Set.of("aws");
		String jobDescriptionText = """
				Required:
				Docker

				Preferred:
				AWS
				""";

		MatchResult result = matcher.match(resumeSkills, "irrelevant resume text", jobDescriptionText);

		// Flat coverage would be 1 matched / 2 total = 50%. Weighting the matched
		// preferred skill (aws) at only 1x pulls the score below flat coverage.
		assertThat(result.matchScorePercentage()).isEqualTo(33);
		assertThat(result.matchedSkills()).containsExactly("aws");
		assertThat(result.missingSkills()).containsExactly("docker");

		assertThat(result.matchedRequiredSkills()).isEmpty();
		assertThat(result.missingRequiredSkills()).containsExactly("docker");
		assertThat(result.matchedPreferredSkills()).containsExactly("aws");
		assertThat(result.missingPreferredSkills()).isEmpty();
	}

	@Test
	void producesTheSameScoreAsFlatCoverageWhenJobDescriptionHasNoRequiredOrPreferredHeaders() {
		Set<String> resumeSkills = Set.of("java", "docker");
		String jobDescriptionText = "Looking for someone skilled in Java, Docker, Kubernetes and AWS.";

		MatchResult result = matcher.match(resumeSkills, "irrelevant resume text", jobDescriptionText);

		// With no headers, every skill defaults to required, so weighting has no
		// differentiating effect and the score matches plain coverage: 2/4 = 50%.
		assertThat(result.matchScorePercentage()).isEqualTo(50);
		assertThat(result.matchedRequiredSkills()).containsExactlyInAnyOrder("java", "docker");
		assertThat(result.matchedPreferredSkills()).isEmpty();
		assertThat(result.missingPreferredSkills()).isEmpty();
	}

	@Test
	void ignoresResumeTextEntirelyAndOnlyUsesTheSkillSet() {
		Set<String> resumeSkills = Set.of("java");
		String jobDescriptionText = "We need a developer who knows Java and React.";

		MatchResult withNullResumeText = matcher.match(resumeSkills, null, jobDescriptionText);
		MatchResult withUnrelatedResumeText = matcher.match(resumeSkills, "completely unrelated text", jobDescriptionText);

		assertThat(withNullResumeText.matchScorePercentage())
				.isEqualTo(withUnrelatedResumeText.matchScorePercentage());
		assertThat(withNullResumeText.matchedSkills()).isEqualTo(withUnrelatedResumeText.matchedSkills());
	}

}
