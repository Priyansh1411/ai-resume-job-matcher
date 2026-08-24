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
	private final TokenRevocationService tokenRevocationService = new TokenRevocationService();
	private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, tokenRevocationService);

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

	@Test
	void leavesTheSecurityContextEmptyWhenTheTokenHasBeenRevoked() throws Exception {
		String token = jwtService.generateToken("user-1");
		JwtClaims claims = jwtService.validateAndGetClaims(token).orElseThrow();
		tokenRevocationService.revoke(claims.tokenId(), claims.expiresAt());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resumes/r1/profile");
		request.addHeader("Authorization", "Bearer " + token);

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
	}

	@Test
	void doesNotRejectTheRequestEvenWithARevokedToken() throws Exception {
		String token = jwtService.generateToken("user-1");
		JwtClaims claims = jwtService.validateAndGetClaims(token).orElseThrow();
		tokenRevocationService.revoke(claims.tokenId(), claims.expiresAt());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resumes/r1/profile");
		request.addHeader("Authorization", "Bearer " + token);
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(request, response, new MockFilterChain());

		// Same reasoning as the invalid-token case above: this filter never blocks a
		// request itself - a revoked token just leaves the context unauthenticated,
		// same as an expired or malformed one, and SecurityConfiguration is what
		// actually turns that into a 401 for endpoints that require it.
		assertThat(response.getStatus()).isEqualTo(200);
	}

	@Test
	void populatesTheSecurityContextForATokenThatHasNotBeenRevoked() throws Exception {
		String token = jwtService.generateToken("user-1");
		JwtClaims claims = jwtService.validateAndGetClaims(token).orElseThrow();

		// A different token's id is revoked - must not affect this one.
		tokenRevocationService.revoke("some-other-token-id", claims.expiresAt());

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/resumes/r1/profile");
		request.addHeader("Authorization", "Bearer " + token);

		filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

		assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
		assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo("user-1");
	}

}
