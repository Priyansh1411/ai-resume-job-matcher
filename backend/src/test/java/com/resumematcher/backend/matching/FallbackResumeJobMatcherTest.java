package com.resumematcher.backend.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

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

	@Test
	void returnsThePrimaryMatchersResultWhenItSucceeds() {
		MatchResult primaryResult = new MatchResult(90, Set.of("java"), Set.of(), Set.of("java"), Set.of(), Set.of(), Set.of());
		when(primaryMatcher.match(Set.of("java"), "resume text", "job text")).thenReturn(primaryResult);

		FallbackResumeJobMatcher matcher = new FallbackResumeJobMatcher(primaryMatcher, fallbackMatcher);

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

		FallbackResumeJobMatcher matcher = new FallbackResumeJobMatcher(primaryMatcher, fallbackMatcher);

		MatchResult result = matcher.match(Set.of("java"), "resume text", "job text");

		assertThat(result).isSameAs(fallbackResult);
	}

	@Test
	void doesNotSwallowUnrelatedRuntimeExceptionsFromThePrimaryMatcher() {
		when(primaryMatcher.match(Set.of("java"), "resume text", "job text"))
				.thenThrow(new IllegalStateException("unexpected programming error"));

		FallbackResumeJobMatcher matcher = new FallbackResumeJobMatcher(primaryMatcher, fallbackMatcher);

		assertThatThrownBy(() -> matcher.match(Set.of("java"), "resume text", "job text"))
				.isInstanceOf(IllegalStateException.class);
		verifyNoInteractions(fallbackMatcher);
	}

}
