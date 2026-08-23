package com.resumematcher.backend.ratelimit;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enforces per-client rate limits on the resume API. Applies to every
 * /api/resumes/** request (health endpoints are never touched), with a stricter,
 * additional budget on the two endpoints that call OpenAI (/match, /analysis).
 *
 * <p>Not a {@code @Component}: it's registered explicitly via
 * {@link RateLimitingConfiguration} instead, so {@code @WebMvcTest} slices - which
 * scan for Filter-typed beans but exclude plain @Service beans - never try to wire
 * it up without {@link RateLimiterService} being available.
 */
public class RateLimitingFilter extends OncePerRequestFilter {

	private static final String RESUME_API_PREFIX = "/api/resumes/";
	private static final Pattern OPENAI_TIER_PATH = Pattern.compile("^/api/resumes/[^/]+/(match|analysis)$");

	private final RateLimiterService rateLimiterService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public RateLimitingFilter(RateLimiterService rateLimiterService) {
		this.rateLimiterService = rateLimiterService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String path = request.getRequestURI();
		if (!path.startsWith(RESUME_API_PREFIX)) {
			filterChain.doFilter(request, response);
			return;
		}

		// Deliberately uses only the socket's remote address, never a client-supplied
		// header like X-Forwarded-For: this app isn't deployed behind a reverse proxy
		// today, and trusting such a header would let a client bypass its own limit
		// simply by sending a different value on each request.
		String clientKey = request.getRemoteAddr();

		RateLimitDecision generalDecision = rateLimiterService.tryConsume(clientKey, RateLimitTier.GENERAL);
		if (!generalDecision.allowed()) {
			writeRejection(response, generalDecision.retryAfterSeconds());
			return;
		}

		if (OPENAI_TIER_PATH.matcher(path).matches()) {
			RateLimitDecision openAiDecision = rateLimiterService.tryConsume(clientKey, RateLimitTier.OPENAI);
			if (!openAiDecision.allowed()) {
				writeRejection(response, openAiDecision.retryAfterSeconds());
				return;
			}
		}

		filterChain.doFilter(request, response);
	}

	private void writeRejection(HttpServletResponse response, long retryAfterSeconds) throws IOException {
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write(
				objectMapper.writeValueAsString(Map.of("error", "Too many requests. Please try again later.")));
	}

}
