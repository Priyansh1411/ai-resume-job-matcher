package com.resumematcher.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class JsonAuthenticationEntryPointTest {

	private final JsonAuthenticationEntryPoint entryPoint = new JsonAuthenticationEntryPoint();

	@Test
	void returnsUnauthorizedWithAConsistentJsonBody() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		entryPoint.commence(new MockHttpServletRequest(), response, new BadCredentialsException("irrelevant"));

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentType()).startsWith("application/json");
		assertThat(response.getContentAsString()).isEqualTo(
				"{\"error\":\"Authentication is required to access this resource\"}");
	}

}
