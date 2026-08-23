package com.resumematcher.backend.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Parses a Bearer JWT if present and populates the SecurityContext with the
 * authenticated user id. Never rejects a request itself - which endpoints
 * require authentication is SecurityConfiguration's job, not this filter's.
 *
 * <p>Not a {@code @Component}: registered explicitly inside SecurityConfiguration's
 * filter chain instead, matching how RateLimitingFilter avoids @WebMvcTest slice
 * auto-discovery pulling in a dependency the slice doesn't provide.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String header = request.getHeader("Authorization");

		if (header != null && header.startsWith(BEARER_PREFIX)) {
			String token = header.substring(BEARER_PREFIX.length());
			Optional<String> userId = jwtService.validateAndGetUserId(token);
			userId.ifPresent(id -> SecurityContextHolder.getContext()
					.setAuthentication(new UsernamePasswordAuthenticationToken(id, null, List.of())));
		}

		filterChain.doFilter(request, response);
	}

}
