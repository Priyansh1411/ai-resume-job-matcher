package com.resumematcher.backend.observability;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers {@link RequestCorrelationFilter} via an explicit
 * {@link FilterRegistrationBean} - rather than the plain {@code @Bean}-returns-Filter
 * style {@code RateLimitingConfiguration} uses for {@code RateLimitingFilter}/
 * {@code AuthRateLimitingFilter} - because this filter specifically needs to run
 * before Spring Security's own filter chain (registered by Spring Boot at
 * {@code HIGHEST_PRECEDENCE + 100}), not after it like the rate limiters do.
 * {@code HIGHEST_PRECEDENCE} guarantees that ordering, so a correlation id
 * exists even for a 401/403 that Spring Security's own entry point/access-denied
 * handler produces before any of this app's other filters run.
 */
@Configuration
public class RequestCorrelationConfiguration {

	@Bean
	public FilterRegistrationBean<RequestCorrelationFilter> requestCorrelationFilter() {
		FilterRegistrationBean<RequestCorrelationFilter> registration =
				new FilterRegistrationBean<>(new RequestCorrelationFilter());
		registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
		return registration;
	}

}
