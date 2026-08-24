package com.resumematcher.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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

	public SecurityConfiguration(JwtService jwtService) {
		this.jwtService = jwtService;
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
						.requestMatchers("/api/health", "/api/database/health").permitAll()
						.requestMatchers("/api/resumes/**").authenticated()
						.anyRequest().permitAll())
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(new JsonAuthenticationEntryPoint())
						.accessDeniedHandler(new JsonAccessDeniedHandler()))
				.addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

}
