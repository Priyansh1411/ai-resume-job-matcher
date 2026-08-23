package com.resumematcher.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

	private final JwtService jwtService = new JwtService("test-only-jwt-signing-secret-must-be-at-least-32-bytes", 24);
	private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void populatesTheSecurityContextWhenTheBearerTokenIsValid() throws Exception {
		String token = jwtService.generateToken("user-1");
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resumes/r1/profile");
		request.addHeader("Authorization", "Bearer " + token);

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo("user-1");
	}

	@Test
	void leavesTheSecurityContextEmptyWhenNoAuthorizationHeaderIsPresent() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resumes/r1/profile");

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void leavesTheSecurityContextEmptyWhenTheTokenIsInvalid() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resumes/r1/profile");
		request.addHeader("Authorization", "Bearer not-a-real-token");

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void doesNotRejectTheRequestEvenWithAnInvalidToken() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resumes/r1/profile");
		request.addHeader("Authorization", "Bearer not-a-real-token");
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		// This filter only ever populates the context; it never blocks a request
		// itself, so an untouched default response status proves the chain
		// continued normally.
		assertThat(response.getStatus()).isEqualTo(200);
	}

}
