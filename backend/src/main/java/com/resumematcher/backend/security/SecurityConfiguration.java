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
 * Phase 1 of the authentication milestone: foundation only. No endpoint requires
 * authentication yet (permitAll on every request) - JwtAuthenticationFilter still
 * runs on every request so a valid token populates the SecurityContext, but
 * nothing enforces it. Requiring authentication on the resume endpoints, and
 * adding resume-ownership checks, is Phase 2.
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
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

}
