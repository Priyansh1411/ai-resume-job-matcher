package com.resumematcher.backend.observability;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Stamps every request with a correlation id so log lines from the same
 * request can be tied together, even across the Jwt/rate-limiting filters and
 * Spring Security's own 401/403 handling. Registered to run before all of
 * those (see {@link RequestCorrelationConfiguration}) specifically so the id
 * - and the {@code X-Request-Id} response header - exist no matter which
 * layer ultimately produces the response (401, 403, 404, 429, or 500).
 *
 * <p>An incoming {@code X-Request-Id} header is reused only if it's a
 * well-formed UUID; anything else (missing, malformed, or an attempt to stuff
 * arbitrary attacker-controlled text into the logs via this header) gets a
 * freshly generated one instead, so log lines and the response header only
 * ever contain a value this filter generated or explicitly validated.
 *
 * <p>Not a {@code @Component}, matching {@link com.resumematcher.backend.ratelimit.RateLimitingFilter}'s
 * and {@link com.resumematcher.backend.security.JwtAuthenticationFilter}'s pattern: registered explicitly
 * via {@link RequestCorrelationConfiguration} instead, so @WebMvcTest slices don't auto-discover it.
 */
public class RequestCorrelationFilter extends OncePerRequestFilter {

	public static final String REQUEST_ID_HEADER = "X-Request-Id";
	public static final String REQUEST_ID_MDC_KEY = "requestId";

	private static final Pattern UUID_PATTERN = Pattern.compile(
			"^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));

		MDC.put(REQUEST_ID_MDC_KEY, requestId);
		// Set before chain.doFilter, not after: downstream layers (Spring Security's
		// entry point/access-denied handler, the rate-limit filters, GlobalExceptionHandler)
		// write and commit the response themselves, so a header added after doFilter
		// returns would never make it onto a 401/403/404/429/500 response.
		response.setHeader(REQUEST_ID_HEADER, requestId);
		try {
			filterChain.doFilter(request, response);
		} finally {
			// Tomcat reuses request-handling threads across requests, so leaving this
			// behind would let the next, unrelated request on this thread inherit a
			// stale requestId until it set its own - clearing here closes that window.
			MDC.remove(REQUEST_ID_MDC_KEY);
		}
	}

	private String resolveRequestId(String headerValue) {
		if (headerValue != null && UUID_PATTERN.matcher(headerValue).matches()) {
			return headerValue;
		}
		return UUID.randomUUID().toString();
	}

}
