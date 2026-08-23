package com.resumematcher.backend.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Instrumentation for the rate limiter, kept separate from {@link OpenAiMetrics}
 * since it measures a distinct concern (request admission, not provider usage).
 */
@Component
public class RateLimitMetrics {

	private final MeterRegistry meterRegistry;

	public RateLimitMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void recordAllowed(String tier) {
		Counter.builder("rate_limit.allowed")
				.tag("tier", tier)
				.register(meterRegistry)
				.increment();
	}

	public void recordRejected(String tier) {
		Counter.builder("rate_limit.rejected")
				.tag("tier", tier)
				.register(meterRegistry)
				.increment();
	}

}
