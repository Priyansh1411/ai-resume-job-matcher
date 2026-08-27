package com.resumematcher.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/**
 * Phase 2 of the authentication milestone: /api/resumes/** now requires
 * authentication. Resource ownership (does this user own this specific resume)
 * is not something URL-pattern rules can express, so that check lives in the
 * service layer (ResumeJobMatchService, ResumeAnalysisService,
 * ResumeProfileQueryService), not here.
 */
@Configuration
public class SecurityConfiguration {

	private final JwtService jwtService;
	private final TokenRevocationService tokenRevocationService;

	public SecurityConfiguration(JwtService jwtService, TokenRevocationService tokenRevocationService) {
		this.jwtService = jwtService;
		this.tokenRevocationService = tokenRevocationService;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
						.requestMatchers("/api/auth/logout").authenticated()
						.requestMatchers("/api/health", "/api/database/health").permitAll()
						.requestMatchers("/api/resumes/**").authenticated()
						// /actuator/health is deliberately left to fall through to permitAll
						// below (see application.properties) - only prometheus, which carries
						// DB pool internals, per-route timing, and this app's own OpenAiMetrics/
						// RateLimitMetrics/ResumeProcessingMetrics, needs a rule here. Reuses
						// the same JWT scheme as /api/resumes/** rather than a dedicated
						// credential: any authenticated app user (not just an operator) can
						// read it, since the app has no role/authority concept yet - an
						// accepted, documented tradeoff for this phase, not a gap closed here.
						.requestMatchers("/actuator/prometheus").authenticated()
						.anyRequest().permitAll())
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(new JsonAuthenticationEntryPoint())
						.accessDeniedHandler(new JsonAccessDeniedHandler()))
				// Referrer-Policy isn't part of Spring Security's default header set,
				// unlike X-Content-Type-Options/X-Frame-Options/X-XSS-Protection, which
				// are already on by default with no configuration needed and so don't
				// appear anywhere in this file. STRICT_ORIGIN_WHEN_CROSS_ORIGIN matches
				// what modern browsers already do absent any policy, so this makes the
				// app's intent explicit and keeps behavior consistent on older browsers
				// too, rather than changing what's actually sent.
				.headers(headers -> headers.referrerPolicy(
						referrerPolicy -> referrerPolicy.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)))
				.addFilterBefore(new JwtAuthenticationFilter(jwtService, tokenRevocationService),
						UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

}
