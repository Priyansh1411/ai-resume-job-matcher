package com.resumematcher.backend.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.resumematcher.backend.observability.RateLimitMetrics;
import com.resumematcher.backend.security.CurrentUserProvider;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class RateLimitingFilterTest {

	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
	private final CurrentUserProvider currentUserProvider = new CurrentUserProvider();

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	private RateLimitingFilter newFilter(int generalRpm, int openAiRpm) {
		RateLimiterService service =
				new RateLimiterService(true, generalRpm, openAiRpm, new RateLimitMetrics(meterRegistry));
		return new RateLimitingFilter(service, currentUserProvider);
	}

	private void authenticateAs(String userId) {
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(userId, null, List.of()));
	}

	@Test
	void blocksTheSameIpAfterItExceedsTheGeneralLimit() throws Exception {
		RateLimitingFilter filter = newFilter(1, 10);

		MockHttpServletResponse firstResponse = new MockHttpServletResponse();
		filter.doFilter(resumeRequest("GET", "/api/resumes/r1/profile", "192.168.1.1"), firstResponse,
				new MockFilterChain());
		assertThat(firstResponse.getStatus()).isEqualTo(200);

		MockHttpServletResponse secondResponse = new MockHttpServletResponse();
		filter.doFilter(resumeRequest("GET", "/api/resumes/r1/profile", "192.168.1.1"), secondResponse,
				new MockFilterChain());
		assertThat(secondResponse.getStatus()).isEqualTo(429);
		assertThat(secondResponse.getHeader("Retry-After")).isNotNull();
		assertThat(secondResponse.getContentAsString()).contains("Too many requests");
	}

	@Test
	void isolatesDifferentIpsFromEachOther() throws Exception {
		RateLimitingFilter filter = newFilter(1, 10);

		filter.doFilter(resumeRequest("GET", "/api/resumes/r1/profile", "192.168.1.1"),
				new MockHttpServletResponse(), new MockFilterChain());

		MockHttpServletResponse secondIpResponse = new MockHttpServletResponse();
		filter.doFilter(resumeRequest("GET", "/api/resumes/r1/profile", "192.168.1.2"), secondIpResponse,
				new MockFilterChain());

		assertThat(secondIpResponse.getStatus()).isEqualTo(200);
	}

	@Test
	void doesNotTrustAnXForwardedForHeaderForClientIdentity() throws Exception {
		RateLimitingFilter filter = newFilter(1, 10);

		MockHttpServletRequest first = resumeRequest("GET", "/api/resumes/r1/profile", "192.168.1.1");
		first.addHeader("X-Forwarded-For", "9.9.9.9");
		filter.doFilter(first, new MockHttpServletResponse(), new MockFilterChain());

		// Same real remote address, but a different spoofed X-Forwarded-For value on
		// the second request - must still be treated as the same client and blocked,
		// proving the header is never read for rate-limit identity.
		MockHttpServletRequest second = resumeRequest("GET", "/api/resumes/r1/profile", "192.168.1.1");
		second.addHeader("X-Forwarded-For", "8.8.8.8");
		MockHttpServletResponse secondResponse = new MockHttpServletResponse();
		filter.doFilter(second, secondResponse, new MockFilterChain());

		assertThat(secondResponse.getStatus()).isEqualTo(429);
	}

	@Test
	void enforcesTheStricterOpenAiTierOnTheMatchEndpoint() throws Exception {
		RateLimitingFilter filter = newFilter(100, 1);

		filter.doFilter(resumeRequest("POST", "/api/resumes/r1/match", "192.168.1.1"),
				new MockHttpServletResponse(), new MockFilterChain());

		MockHttpServletResponse secondResponse = new MockHttpServletResponse();
		filter.doFilter(resumeRequest("POST", "/api/resumes/r1/match", "192.168.1.1"), secondResponse,
				new MockFilterChain());

		assertThat(secondResponse.getStatus()).isEqualTo(429);
	}

	@Test
	void doesNotApplyTheOpenAiTierToTheProfileEndpoint() throws Exception {
		RateLimitingFilter filter = newFilter(100, 1);

		filter.doFilter(resumeRequest("GET", "/api/resumes/r1/profile", "192.168.1.1"),
				new MockHttpServletResponse(), new MockFilterChain());

		// The OpenAI tier's budget of 1 would already be exhausted here if it applied
		// to /profile too, so a second successful call proves it doesn't.
		MockHttpServletResponse secondResponse = new MockHttpServletResponse();
		filter.doFilter(resumeRequest("GET", "/api/resumes/r1/profile", "192.168.1.1"), secondResponse,
				new MockFilterChain());

		assertThat(secondResponse.getStatus()).isEqualTo(200);
	}

	@Test
	void skipsHealthEndpointsEntirely() throws Exception {
		RateLimitingFilter filter = newFilter(1, 1);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/health");
		request.setRemoteAddr("192.168.1.1");
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void usesTheAuthenticatedUserIdInsteadOfIpWhenAuthenticated() throws Exception {
		RateLimitingFilter filter = newFilter(1, 10);

		authenticateAs("user-A");
		filter.doFilter(resumeRequest("GET", "/api/resumes/r1/profile", "192.168.1.1"),
				new MockHttpServletResponse(), new MockFilterChain());

		// Same IP, but a different authenticated user - must be an independent
		// budget, proving the key is the user id, not the shared IP.
		authenticateAs("user-B");
		MockHttpServletResponse secondResponse = new MockHttpServletResponse();
		filter.doFilter(resumeRequest("GET", "/api/resumes/r1/profile", "192.168.1.1"), secondResponse,
				new MockFilterChain());

		assertThat(secondResponse.getStatus()).isEqualTo(200);
	}

	@Test
	void sharesOneBudgetForTheSameAuthenticatedUserAcrossDifferentIps() throws Exception {
		RateLimitingFilter filter = newFilter(1, 10);

		authenticateAs("user-A");
		filter.doFilter(resumeRequest("GET", "/api/resumes/r1/profile", "192.168.1.1"),
				new MockHttpServletResponse(), new MockFilterChain());

		// Same authenticated user, different IP - still the same budget, so this
		// is blocked even though the IP alone would look like a fresh client.
		MockHttpServletResponse secondResponse = new MockHttpServletResponse();
		filter.doFilter(resumeRequest("GET", "/api/resumes/r1/profile", "192.168.1.2"), secondResponse,
				new MockFilterChain());

		assertThat(secondResponse.getStatus()).isEqualTo(429);
	}

	private MockHttpServletRequest resumeRequest(String method, String uri, String remoteAddr) {
		MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
		request.setRemoteAddr(remoteAddr);
		return request;
	}

}
