package com.resumematcher.backend.ratelimit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link RateLimitingFilter} via an explicit @Bean method (rather than
 * making the filter itself a @Component). Plain @Configuration classes like this
 * one are excluded from @WebMvcTest slices, so the filter - and its RateLimiterService
 * dependency - never get pulled into a controller-slice test.
 */
@Configuration
public class RateLimitingConfiguration {

	@Bean
	public RateLimitingFilter rateLimitingFilter(RateLimiterService rateLimiterService) {
		return new RateLimitingFilter(rateLimiterService);
	}

}
