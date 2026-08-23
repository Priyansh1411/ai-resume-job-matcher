package com.resumematcher.backend.observability;

import java.time.Duration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Single point of instrumentation for OpenAI usage and semantic-matching fallback
 * behavior, so counters/timers aren't scattered ad hoc across the matching and
 * analysis packages.
 */
@Component
public class OpenAiMetrics {

	private final MeterRegistry meterRegistry;

	public OpenAiMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void recordEmbeddingCall(Duration latency, boolean success) {
		String outcome = success ? "success" : "failure";
		Counter.builder("openai.embedding.calls")
				.tag("outcome", outcome)
				.register(meterRegistry)
				.increment();
		Timer.builder("openai.embedding.latency")
				.tag("outcome", outcome)
				.register(meterRegistry)
				.record(latency);
	}

	public void recordChatCompletionCall(Duration latency, boolean success) {
		String outcome = success ? "success" : "failure";
		Counter.builder("openai.chat.calls")
				.tag("outcome", outcome)
				.register(meterRegistry)
				.increment();
		Timer.builder("openai.chat.latency")
				.tag("outcome", outcome)
				.register(meterRegistry)
				.record(latency);
	}

	public void recordSemanticMatchAttempt() {
		Counter.builder("matching.semantic.attempts")
				.register(meterRegistry)
				.increment();
	}

	public void recordSemanticFallback(String reason) {
		Counter.builder("matching.semantic.fallback")
				.tag("reason", reason)
				.register(meterRegistry)
				.increment();
	}

}
