package com.resumematcher.backend.ratelimit;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Enforces per-IP rate limits on the two pre-authentication auth endpoints.
 * Kept separate from {@link RateLimitingFilter} (which is documented and scoped
 * to the resume API) rather than folding auth paths into it, so each filter
 * keeps a single, obvious responsibility.
 *
 * <p>Login and register get independent budgets - see {@link RateLimitTier#AUTH_LOGIN}
 * and {@link RateLimitTier#AUTH_REGISTER} - since they have different abuse
 * profiles (brute-force credential guessing vs. mass account creation /
 * email-enumeration) and shouldn't share a budget: hammering one shouldn't be
 * able to lock a real user out of the other from the same IP.
 *
 * <p>Always keyed by IP, never by authenticated user id like
 * {@link RateLimitingFilter} prefers - these endpoints are inherently
 * pre-authentication, so there's no user identity yet to key on. Never trusts
 * {@code X-Forwarded-For}, for the same spoofing reason as {@link RateLimitingFilter}.
 *
 * <p>Not a {@code @Component}: registered explicitly via
 * {@link RateLimitingConfiguration}, matching how {@link RateLimitingFilter}
 * avoids @WebMvcTest slice auto-discovery pulling in a dependency the slice
 * doesn't provide.
 */
public class AuthRateLimitingFilter extends OncePerRequestFilter {

	private static final String LOGIN_PATH = "/api/auth/login";
	private static final String REGISTER_PATH = "/api/auth/register";

	private final RateLimiterService rateLimiterService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public AuthRateLimitingFilter(RateLimiterService rateLimiterService) {
		this.rateLimiterService = rateLimiterService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		RateLimitTier tier = tierFor(request);
		if (tier == null) {
			filterChain.doFilter(request, response);
			return;
		}

		String clientKey = "ip:" + request.getRemoteAddr();
		RateLimitDecision decision = rateLimiterService.tryConsume(clientKey, tier);
		if (!decision.allowed()) {
			writeRejection(response, decision.retryAfterSeconds());
			return;
		}

		filterChain.doFilter(request, response);
	}

	private RateLimitTier tierFor(HttpServletRequest request) {
		if (!HttpMethod.POST.matches(request.getMethod())) {
			return null;
		}
		String path = request.getRequestURI();
		if (LOGIN_PATH.equals(path)) {
			return RateLimitTier.AUTH_LOGIN;
		}
		if (REGISTER_PATH.equals(path)) {
			return RateLimitTier.AUTH_REGISTER;
		}
		return null;
	}

	private void writeRejection(HttpServletResponse response, long retryAfterSeconds) throws IOException {
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write(
				objectMapper.writeValueAsString(Map.of("error", "Too many requests. Please try again later.")));
	}

}
