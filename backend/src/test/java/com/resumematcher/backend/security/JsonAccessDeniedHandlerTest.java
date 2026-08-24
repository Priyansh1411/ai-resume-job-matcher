package com.resumematcher.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class JsonAccessDeniedHandlerTest {

	private final JsonAccessDeniedHandler accessDeniedHandler = new JsonAccessDeniedHandler();

	@Test
	void returnsForbiddenWithAConsistentJsonBody() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		accessDeniedHandler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("irrelevant"));

		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getContentType()).startsWith("application/json");
		assertThat(response.getContentAsString()).isEqualTo(
				"{\"error\":\"You do not have permission to access this resource\"}");
	}

}
