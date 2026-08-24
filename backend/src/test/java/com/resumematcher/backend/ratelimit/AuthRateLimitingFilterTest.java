package com.resumematcher.backend.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.resumematcher.backend.observability.RateLimitMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthRateLimitingFilterTest {

	private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

	private AuthRateLimitingFilter newFilter(int loginRpm, int registerRpm) {
		RateLimiterService service =
				new RateLimiterService(true, 100, 100, loginRpm, registerRpm, new RateLimitMetrics(meterRegistry));
		return new AuthRateLimitingFilter(service);
	}

	private MockHttpServletRequest authRequest(String path, String remoteAddr) {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
		request.setRemoteAddr(remoteAddr);
		return request;
	}

	@Test
	void blocksTheSameIpAfterItExceedsTheLoginLimit() throws Exception {
		AuthRateLimitingFilter filter = newFilter(1, 10);

		MockHttpServletResponse firstResponse = new MockHttpServletResponse();
		filter.doFilter(authRequest("/api/auth/login", "192.168.1.1"), firstResponse, new MockFilterChain());
		assertThat(firstResponse.getStatus()).isEqualTo(200);

		MockHttpServletResponse secondResponse = new MockHttpServletResponse();
		filter.doFilter(authRequest("/api/auth/login", "192.168.1.1"), secondResponse, new MockFilterChain());
		assertThat(secondResponse.getStatus()).isEqualTo(429);
		assertThat(secondResponse.getHeader("Retry-After")).isNotNull();
		assertThat(secondResponse.getContentAsString()).contains("Too many requests");
	}

	@Test
	void blocksTheSameIpAfterItExceedsTheRegisterLimit() throws Exception {
		AuthRateLimitingFilter filter = newFilter(10, 1);

		MockHttpServletResponse firstResponse = new MockHttpServletResponse();
		filter.doFilter(authRequest("/api/auth/register", "192.168.1.1"), firstResponse, new MockFilterChain());
		assertThat(firstResponse.getStatus()).isEqualTo(200);

		MockHttpServletResponse secondResponse = new MockHttpServletResponse();
		filter.doFilter(authRequest("/api/auth/register", "192.168.1.1"), secondResponse, new MockFilterChain());
		assertThat(secondResponse.getStatus()).isEqualTo(429);
		assertThat(secondResponse.getHeader("Retry-After")).isNotNull();
		assertThat(secondResponse.getContentAsString()).contains("Too many requests");
	}

	@Test
	void isolatesDifferentIpsFromEachOther() throws Exception {
		AuthRateLimitingFilter filter = newFilter(1, 10);

		filter.doFilter(authRequest("/api/auth/login", "192.168.1.1"), new MockHttpServletResponse(),
				new MockFilterChain());

		MockHttpServletResponse secondIpResponse = new MockHttpServletResponse();
		filter.doFilter(authRequest("/api/auth/login", "192.168.1.2"), secondIpResponse, new MockFilterChain());

		assertThat(secondIpResponse.getStatus()).isEqualTo(200);
	}

	@Test
	void doesNotTrustAnXForwardedForHeaderForClientIdentity() throws Exception {
		AuthRateLimitingFilter filter = newFilter(1, 10);

		MockHttpServletRequest first = authRequest("/api/auth/login", "192.168.1.1");
		first.addHeader("X-Forwarded-For", "9.9.9.9");
		filter.doFilter(first, new MockHttpServletResponse(), new MockFilterChain());

		// Same real remote address, but a different spoofed X-Forwarded-For value on
		// the second request - must still be treated as the same client and blocked,
		// proving the header is never read for rate-limit identity.
		MockHttpServletRequest second = authRequest("/api/auth/login", "192.168.1.1");
		second.addHeader("X-Forwarded-For", "8.8.8.8");
		MockHttpServletResponse secondResponse = new MockHttpServletResponse();
		filter.doFilter(second, secondResponse, new MockFilterChain());

		assertThat(secondResponse.getStatus()).isEqualTo(429);
	}

	@Test
	void tracksLoginAndRegisterAsIndependentBudgetsForTheSameIp() throws Exception {
		AuthRateLimitingFilter filter = newFilter(1, 1);

		filter.doFilter(authRequest("/api/auth/login", "192.168.1.1"), new MockHttpServletResponse(),
				new MockFilterChain());

		// Login's budget for this IP is now exhausted, but register has its own,
		// untouched budget - exhausting one endpoint must not lock out the other.
		MockHttpServletResponse registerResponse = new MockHttpServletResponse();
		filter.doFilter(authRequest("/api/auth/register", "192.168.1.1"), registerResponse, new MockFilterChain());

		assertThat(registerResponse.getStatus()).isEqualTo(200);
	}

	@Test
	void doesNotApplyToUnrelatedPaths() throws Exception {
		AuthRateLimitingFilter filter = newFilter(1, 1);

		filter.doFilter(authRequest("/api/auth/login", "192.168.1.1"), new MockHttpServletResponse(),
				new MockFilterChain());

		// Login's budget for this IP is exhausted, but a request to an unrelated path
		// is never gated by this filter at all, so it must pass through untouched.
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(authRequest("/api/health", "192.168.1.1"), response, new MockFilterChain());

		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void doesNotApplyToNonPostRequestsOnAuthPaths() throws Exception {
		AuthRateLimitingFilter filter = newFilter(1, 1);

		MockHttpServletRequest getRequest = new MockHttpServletRequest("GET", "/api/auth/login");
		getRequest.setRemoteAddr("192.168.1.1");
		MockHttpServletResponse firstResponse = new MockHttpServletResponse();
		filter.doFilter(getRequest, firstResponse, new MockFilterChain());
		assertThat(firstResponse.getStatus()).isEqualTo(200);

		// The GET above must not have consumed the POST /login budget for this IP.
		MockHttpServletResponse postResponse = new MockHttpServletResponse();
		filter.doFilter(authRequest("/api/auth/login", "192.168.1.1"), postResponse, new MockFilterChain());
		assertThat(postResponse.getStatus()).isEqualTo(200);
	}

}
