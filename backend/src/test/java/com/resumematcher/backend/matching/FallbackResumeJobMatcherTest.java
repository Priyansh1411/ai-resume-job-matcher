package com.resumematcher.backend.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import com.resumematcher.backend.observability.OpenAiMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FallbackResumeJobMatcherTest {

	@Mock
	private ResumeJobMatcher primaryMatcher;

	@Mock
	private ResumeJobMatcher fallbackMatcher;

	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
	private final OpenAiMetrics openAiMetrics = new OpenAiMetrics(meterRegistry);

	@Test
	void returnsThePrimaryMatchersResultWhenItSucceeds() {
		MatchResult primaryResult = new MatchResult(90, Set.of("java"), Set.of(), Set.of("java"), Set.of(), Set.of(), Set.of());
		when(primaryMatcher.match(Set.of("java"), "resume text", "job text")).thenReturn(primaryResult);

		FallbackResumeJobMatcher matcher = new FallbackResumeJobMatcher(primaryMatcher, fallbackMatcher, openAiMetrics);

		MatchResult result = matcher.match(Set.of("java"), "resume text", "job text");

		assertThat(result).isSameAs(primaryResult);
		verifyNoInteractions(fallbackMatcher);
	}

	@Test
	void fallsBackToTheSecondMatcherWhenThePrimaryThrowsEmbeddingException() {
		MatchResult fallbackResult = new MatchResult(50, Set.of(), Set.of("java"), Set.of(), Set.of("java"), Set.of(), Set.of());
		when(primaryMatcher.match(Set.of("java"), "resume text", "job text"))
				.thenThrow(new EmbeddingException("provider unreachable"));
		when(fallbackMatcher.match(Set.of("java"), "resume text", "job text")).thenReturn(fallbackResult);

		FallbackResumeJobMatcher matcher = new FallbackResumeJobMatcher(primaryMatcher, fallbackMatcher, openAiMetrics);

		MatchResult result = matcher.match(Set.of("java"), "resume text", "job text");

		assertThat(result).isSameAs(fallbackResult);
	}

	@Test
	void doesNotSwallowUnrelatedRuntimeExceptionsFromThePrimaryMatcher() {
		when(primaryMatcher.match(Set.of("java"), "resume text", "job text"))
				.thenThrow(new IllegalStateException("unexpected programming error"));

		FallbackResumeJobMatcher matcher = new FallbackResumeJobMatcher(primaryMatcher, fallbackMatcher, openAiMetrics);

		assertThatThrownBy(() -> matcher.match(Set.of("java"), "resume text", "job text"))
				.isInstanceOf(IllegalStateException.class);
		verifyNoInteractions(fallbackMatcher);
	}

	@Test
	void recordsASemanticMatchAttemptOnEveryCall() {
		MatchResult primaryResult = new MatchResult(90, Set.of("java"), Set.of(), Set.of("java"), Set.of(), Set.of(), Set.of());
		when(primaryMatcher.match(Set.of("java"), "resume text", "job text")).thenReturn(primaryResult);

		FallbackResumeJobMatcher matcher = new FallbackResumeJobMatcher(primaryMatcher, fallbackMatcher, openAiMetrics);
		matcher.match(Set.of("java"), "resume text", "job text");

		assertThat(meterRegistry.get("matching.semantic.attempts").counter().count()).isEqualTo(1.0);
	}

	@Test
	void recordsASemanticFallbackWhenThePrimaryMatcherFails() {
		when(primaryMatcher.match(Set.of("java"), "resume text", "job text"))
				.thenThrow(new EmbeddingException("provider unreachable"));
		when(fallbackMatcher.match(Set.of("java"), "resume text", "job text"))
				.thenReturn(new MatchResult(50, Set.of(), Set.of("java"), Set.of(), Set.of("java"), Set.of(), Set.of()));

		FallbackResumeJobMatcher matcher = new FallbackResumeJobMatcher(primaryMatcher, fallbackMatcher, openAiMetrics);
		matcher.match(Set.of("java"), "resume text", "job text");

		assertThat(meterRegistry.get("matching.semantic.fallback").counter().count()).isEqualTo(1.0);
	}

	@Test
	void doesNotRecordAFallbackWhenThePrimaryMatcherSucceeds() {
		MatchResult primaryResult = new MatchResult(90, Set.of("java"), Set.of(), Set.of("java"), Set.of(), Set.of(), Set.of());
		when(primaryMatcher.match(Set.of("java"), "resume text", "job text")).thenReturn(primaryResult);

		FallbackResumeJobMatcher matcher = new FallbackResumeJobMatcher(primaryMatcher, fallbackMatcher, openAiMetrics);
		matcher.match(Set.of("java"), "resume text", "job text");

		assertThat(meterRegistry.find("matching.semantic.fallback").counter()).isNull();
	}

}
