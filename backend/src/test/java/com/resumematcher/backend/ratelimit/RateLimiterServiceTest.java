package com.resumematcher.backend.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.resumematcher.backend.observability.RateLimitMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class RateLimiterServiceTest {

	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
	private final RateLimitMetrics rateLimitMetrics = new RateLimitMetrics(meterRegistry);

	@Test
	void blocksTheSameClientAfterItExhaustsItsLimit() {
		RateLimiterService service = new RateLimiterService(true, 2, 10, rateLimitMetrics);

		assertThat(service.tryConsume("192.168.1.1", RateLimitTier.GENERAL).allowed()).isTrue();
		assertThat(service.tryConsume("192.168.1.1", RateLimitTier.GENERAL).allowed()).isTrue();
		assertThat(service.tryConsume("192.168.1.1", RateLimitTier.GENERAL).allowed()).isFalse();
	}

	@Test
	void isolatesDifferentClientsFromEachOther() {
		RateLimiterService service = new RateLimiterService(true, 1, 10, rateLimitMetrics);

		assertThat(service.tryConsume("192.168.1.1", RateLimitTier.GENERAL).allowed()).isTrue();
		assertThat(service.tryConsume("192.168.1.1", RateLimitTier.GENERAL).allowed()).isFalse();

		// A different client key has its own, unaffected budget.
		assertThat(service.tryConsume("192.168.1.2", RateLimitTier.GENERAL).allowed()).isTrue();
	}

	@Test
	void tracksTheGeneralAndOpenAiTiersAsIndependentBudgets() {
		RateLimiterService service = new RateLimiterService(true, 1, 1, rateLimitMetrics);

		assertThat(service.tryConsume("192.168.1.1", RateLimitTier.GENERAL).allowed()).isTrue();
		assertThat(service.tryConsume("192.168.1.1", RateLimitTier.GENERAL).allowed()).isFalse();

		// Same client, different tier: its own independent budget.
		assertThat(service.tryConsume("192.168.1.1", RateLimitTier.OPENAI).allowed()).isTrue();
	}

	@Test
	void allowsEveryRequestWhenDisabled() {
		RateLimiterService service = new RateLimiterService(false, 1, 1, rateLimitMetrics);

		for (int i = 0; i < 5; i++) {
			assertThat(service.tryConsume("192.168.1.1", RateLimitTier.GENERAL).allowed()).isTrue();
		}
	}

	@Test
	void recordsAllowedAndRejectedMetricsTaggedByTier() {
		RateLimiterService service = new RateLimiterService(true, 1, 10, rateLimitMetrics);

		service.tryConsume("192.168.1.1", RateLimitTier.GENERAL);
		service.tryConsume("192.168.1.1", RateLimitTier.GENERAL);

		assertThat(meterRegistry.get("rate_limit.allowed").tag("tier", "general").counter().count()).isEqualTo(1.0);
		assertThat(meterRegistry.get("rate_limit.rejected").tag("tier", "general").counter().count()).isEqualTo(1.0);
	}

	@Test
	void includesARetryAfterEstimateWhenRejected() {
		RateLimiterService service = new RateLimiterService(true, 1, 10, rateLimitMetrics);

		service.tryConsume("192.168.1.1", RateLimitTier.GENERAL);
		RateLimitDecision rejected = service.tryConsume("192.168.1.1", RateLimitTier.GENERAL);

		assertThat(rejected.allowed()).isFalse();
		assertThat(rejected.retryAfterSeconds()).isGreaterThan(0);
	}

}
