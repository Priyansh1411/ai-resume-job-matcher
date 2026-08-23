package com.resumematcher.backend.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

class SkillKeywordMatcherTest {

	private final SkillKeywordMatcher matcher = new SkillKeywordMatcher(new SkillDictionary());

	@Test
	void matchesExactCanonicalSkillNames() {
		Set<String> skills = matcher.findSkills("Experienced with Java, Docker and MySQL.");

		assertThat(skills).contains("java", "docker", "mysql");
	}

	@Test
	void resolvesCommonAliasesToTheirCanonicalSkillName() {
		Set<String> skills = matcher.findSkills("Worked with k8s, JS and Postgres in production.");

		assertThat(skills).contains("kubernetes", "javascript", "postgresql");
		assertThat(skills).doesNotContain("k8s", "js", "postgres");
	}

	@Test
	void doesNotReportAnAliasAsASeparateSkillFromItsCanonicalForm() {
		Set<String> skills = matcher.findSkills("Kubernetes and k8s experience, plus JavaScript and JS work.");

		assertThat(skills).containsOnlyOnce("kubernetes");
		assertThat(skills).containsOnlyOnce("javascript");
	}

	@Test
	void matchesSkillsWithPunctuationCorrectly() {
		Set<String> skills = matcher.findSkills("Proficient in C++, C#, .NET and CI/CD pipelines.");

		assertThat(skills).contains("c++", "c#", ".net", "ci/cd");
	}

	@Test
	void returnsEmptySetWhenNoSkillsRecognized() {
		Set<String> skills = matcher.findSkills("This document mentions nothing recognizable at all.");

		assertThat(skills).isEmpty();
	}

	@Test
	void returnsEmptySetForNullInput() {
		assertThat(matcher.findSkills(null)).isEmpty();
	}

}
