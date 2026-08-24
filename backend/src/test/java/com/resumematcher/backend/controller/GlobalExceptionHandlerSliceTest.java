package com.resumematcher.backend.controller;

import static org.mockito.Mockito.when;

import com.resumematcher.backend.security.AuthService;
import com.resumematcher.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * Covers {@link GlobalExceptionHandler}'s generic {@code Exception.class}
 * catch-all specifically - the one case that can't be reached through any real,
 * unmodified endpoint (every existing service already anticipates its own
 * failure modes). A {@code @WebMvcTest} slice with a stubbed service is the
 * same pattern this codebase's other controller tests already use for every
 * other failure path (e.g. AuthControllerTest's
 * loginReturnsUnauthorizedOnWrongPassword stubs AuthService to throw
 * InvalidCredentialsException) - the controller call itself is real, going
 * through the real DispatcherServlet and the real GlobalExceptionHandler bean;
 * only the service's return behavior is stubbed. Kept in its own file rather
 * than added to AuthControllerTest since a single test class can only carry
 * one Spring test slice configuration.
 */
@WebMvcTest(AuthController.class)
class GlobalExceptionHandlerSliceTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private JwtService jwtService;

	@Test
	void genuinelyUnexpectedExceptionReturnsInternalServerErrorWithNoLeakedDetails() throws Exception {
		when(authService.login("jane@example.com", "correct-horse"))
				.thenThrow(new RuntimeException("some internal detail that must never reach the client"));

		// The exact-value assertion below already implies the stubbed exception's
		// own message never appears - a fixed literal can't also contain it. The
		// doesNotExist checks separately guard against a stack trace or exception
		// class name being added as extra fields alongside the message.
		mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"jane@example.com\",\"password\":\"correct-horse\"}"))
				.andExpect(MockMvcResultMatchers.status().isInternalServerError())
				.andExpect(MockMvcResultMatchers.jsonPath("$.error").value("An unexpected error occurred. Please try again."))
				.andExpect(MockMvcResultMatchers.jsonPath("$.trace").doesNotExist())
				.andExpect(MockMvcResultMatchers.jsonPath("$.exception").doesNotExist());
	}

}
