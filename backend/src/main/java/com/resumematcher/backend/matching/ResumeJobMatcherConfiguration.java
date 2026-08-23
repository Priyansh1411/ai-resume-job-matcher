package com.resumematcher.backend.matching;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResumeJobMatcherConfiguration {

	@Bean
	public ResumeJobMatcher resumeJobMatcher(
			@Value("${matching.semantic.enabled:false}") boolean semanticMatchingEnabled,
			KeywordResumeJobMatcher keywordResumeJobMatcher,
			EmbeddingClient embeddingClient) {

		if (!semanticMatchingEnabled) {
			return keywordResumeJobMatcher;
		}

		EmbeddingResumeJobMatcher embeddingResumeJobMatcher =
				new EmbeddingResumeJobMatcher(embeddingClient, keywordResumeJobMatcher);

		return new FallbackResumeJobMatcher(embeddingResumeJobMatcher, keywordResumeJobMatcher);
	}

}
