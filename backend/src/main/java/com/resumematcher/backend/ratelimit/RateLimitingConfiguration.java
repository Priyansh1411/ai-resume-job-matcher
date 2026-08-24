package com.resumematcher.backend.ratelimit;

import com.resumematcher.backend.security.CurrentUserProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers {@link RateLimitingFilter} and {@link AuthRateLimitingFilter} via
 * explicit @Bean methods (rather than making the filters themselves
 * @Component). Plain @Configuration classes like this one are excluded from
 * @WebMvcTest slices, so neither filter - nor their RateLimiterService
 * dependency - ever gets pulled into a controller-slice test.
 */
@Configuration
public class RateLimitingConfiguration {

	@Bean
	public RateLimitingFilter rateLimitingFilter(RateLimiterService rateLimiterService,
			CurrentUserProvider currentUserProvider) {
		return new RateLimitingFilter(rateLimiterService, currentUserProvider);
	}

	@Bean
	public AuthRateLimitingFilter authRateLimitingFilter(RateLimiterService rateLimiterService) {
		return new AuthRateLimitingFilter(rateLimiterService);
	}

}
