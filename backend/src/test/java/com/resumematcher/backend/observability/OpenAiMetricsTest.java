package com.resumematcher.backend.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class OpenAiMetricsTest {

	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
	private final OpenAiMetrics openAiMetrics = new OpenAiMetrics(meterRegistry);

	@Test
	void recordsASuccessfulEmbeddingCall() {
		openAiMetrics.recordEmbeddingCall(Duration.ofMillis(120), true);

		assertThat(meterRegistry.get("openai.embedding.calls").tag("outcome", "success").counter().count())
				.isEqualTo(1.0);
		assertThat(meterRegistry.get("openai.embedding.latency").tag("outcome", "success").timer()
				.totalTime(TimeUnit.MILLISECONDS)).isEqualTo(120.0);
	}

	@Test
	void recordsAFailedEmbeddingCallSeparatelyFromSuccesses() {
		openAiMetrics.recordEmbeddingCall(Duration.ofMillis(50), false);

		assertThat(meterRegistry.get("openai.embedding.calls").tag("outcome", "failure").counter().count())
				.isEqualTo(1.0);
		assertThat(meterRegistry.find("openai.embedding.calls").tag("outcome", "success").counter()).isNull();
	}

	@Test
	void recordsASuccessfulChatCompletionCall() {
		openAiMetrics.recordChatCompletionCall(Duration.ofMillis(300), true);

		assertThat(meterRegistry.get("openai.chat.calls").tag("outcome", "success").counter().count())
				.isEqualTo(1.0);
		assertThat(meterRegistry.get("openai.chat.latency").tag("outcome", "success").timer().count()).isEqualTo(1L);
	}

	@Test
	void recordsAFailedChatCompletionCall() {
		openAiMetrics.recordChatCompletionCall(Duration.ofMillis(80), false);

		assertThat(meterRegistry.get("openai.chat.calls").tag("outcome", "failure").counter().count())
				.isEqualTo(1.0);
	}

	@Test
	void accumulatesSemanticMatchAttemptsAcrossCalls() {
		openAiMetrics.recordSemanticMatchAttempt();
		openAiMetrics.recordSemanticMatchAttempt();
		openAiMetrics.recordSemanticMatchAttempt();

		assertThat(meterRegistry.get("matching.semantic.attempts").counter().count()).isEqualTo(3.0);
	}

	@Test
	void recordsASemanticFallbackTaggedByReason() {
		openAiMetrics.recordSemanticFallback("IOException");

		assertThat(meterRegistry.get("matching.semantic.fallback").tag("reason", "IOException").counter().count())
				.isEqualTo(1.0);
	}

}
