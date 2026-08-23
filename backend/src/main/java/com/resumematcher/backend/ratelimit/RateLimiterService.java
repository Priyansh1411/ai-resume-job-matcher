package com.resumematcher.backend.ratelimit;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

import com.resumematcher.backend.observability.RateLimitMetrics;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Owns all bucket storage/enforcement. Callers only see {@link RateLimitTier} and
 * {@link RateLimitDecision} - no Bucket4j type crosses this boundary - so a future
 * move to a distributed store (e.g. Redis-backed buckets) only touches this class,
 * never {@link RateLimitingFilter} or any controller.
 */
@Service
public class RateLimiterService {

	private final boolean enabled;
	private final int generalRequestsPerMinute;
	private final int openAiRequestsPerMinute;
	private final RateLimitMetrics rateLimitMetrics;

	private final ConcurrentHashMap<String, Bucket> generalBuckets = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Bucket> openAiBuckets = new ConcurrentHashMap<>();

	public RateLimiterService(
			@Value("${rate-limit.enabled:true}") boolean enabled,
			@Value("${rate-limit.general.requests-per-minute:60}") int generalRequestsPerMinute,
			@Value("${rate-limit.openai.requests-per-minute:10}") int openAiRequestsPerMinute,
			RateLimitMetrics rateLimitMetrics) {
		this.enabled = enabled;
		this.generalRequestsPerMinute = generalRequestsPerMinute;
		this.openAiRequestsPerMinute = openAiRequestsPerMinute;
		this.rateLimitMetrics = rateLimitMetrics;
	}

	public RateLimitDecision tryConsume(String clientKey, RateLimitTier tier) {
		if (!enabled) {
			return new RateLimitDecision(true, 0);
		}

		Bucket bucket = bucketFor(clientKey, tier);
		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
		String tierTag = tier.name().toLowerCase();

		if (probe.isConsumed()) {
			rateLimitMetrics.recordAllowed(tierTag);
			return new RateLimitDecision(true, 0);
		}

		rateLimitMetrics.recordRejected(tierTag);
		long retryAfterSeconds = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000);
		return new RateLimitDecision(false, retryAfterSeconds);
	}

	private Bucket bucketFor(String clientKey, RateLimitTier tier) {
		return switch (tier) {
			case GENERAL -> generalBuckets.computeIfAbsent(clientKey, key -> newBucket(generalRequestsPerMinute));
			case OPENAI -> openAiBuckets.computeIfAbsent(clientKey, key -> newBucket(openAiRequestsPerMinute));
		};
	}

	private Bucket newBucket(int requestsPerMinute) {
		Bandwidth limit = Bandwidth.builder()
				.capacity(requestsPerMinute)
				.refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
				.build();
		return Bucket.builder().addLimit(limit).build();
	}

}
