package com.resumematcher.backend.matching;

import static org.assertj.core.api.Assertions.assertThat;

import com.resumematcher.backend.profile.SkillDictionary;
import com.resumematcher.backend.profile.SkillKeywordMatcher;
import org.junit.jupiter.api.Test;

class ResumeJobMatcherConfigurationTest {

	private final ResumeJobMatcherConfiguration configuration = new ResumeJobMatcherConfiguration();

	private final KeywordResumeJobMatcher keywordResumeJobMatcher = new KeywordResumeJobMatcher(
			new JobDescriptionRequirementParser(new SkillKeywordMatcher(new SkillDictionary())));

	@Test
	void usesTheKeywordMatcherDirectlyWhenSemanticMatchingIsDisabled() {
		ResumeJobMatcher result = configuration.resumeJobMatcher(false, keywordResumeJobMatcher, null);

		assertThat(result).isSameAs(keywordResumeJobMatcher);
	}

	@Test
	void wrapsAFallbackAroundTheEmbeddingMatcherWhenSemanticMatchingIsEnabled() {
		EmbeddingClient stubEmbeddingClient = text -> new float[] { 1f, 0f };

		ResumeJobMatcher result = configuration.resumeJobMatcher(true, keywordResumeJobMatcher, stubEmbeddingClient);

		assertThat(result).isInstanceOf(FallbackResumeJobMatcher.class);
	}

}
