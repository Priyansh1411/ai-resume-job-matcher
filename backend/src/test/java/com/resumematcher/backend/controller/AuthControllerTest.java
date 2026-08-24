package com.resumematcher.backend.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import com.resumematcher.backend.dto.AuthResponse;
import com.resumematcher.backend.security.AuthService;
import com.resumematcher.backend.security.DuplicateEmailException;
import com.resumematcher.backend.security.InvalidCredentialsException;
import com.resumematcher.backend.security.JwtClaims;
import com.resumematcher.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AuthController authController;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private JwtService jwtService;

	@Test
	void registerReturnsCreatedWithAToken() throws Exception {
		when(authService.register("jane@example.com", "correct-horse")).thenReturn(new AuthResponse("token-123"));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"jane@example.com\",\"password\":\"correct-horse\"}"))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andExpect(MockMvcResultMatchers.jsonPath("$.token").value("token-123"));
	}

	@Test
	void registerReturnsBadRequestWhenPasswordIsTooShort() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"jane@example.com\",\"password\":\"short\"}"))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.error").exists());
	}

	@Test
	void registerReturnsConflictWhenEmailIsAlreadyTaken() throws Exception {
		when(authService.register("jane@example.com", "correct-horse"))
				.thenThrow(new DuplicateEmailException("An account with this email already exists"));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"jane@example.com\",\"password\":\"correct-horse\"}"))
				.andExpect(MockMvcResultMatchers.status().isConflict());
	}

	@Test
	void loginReturnsOkWithATokenOnSuccess() throws Exception {
		when(authService.login("jane@example.com", "correct-horse")).thenReturn(new AuthResponse("token-123"));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"jane@example.com\",\"password\":\"correct-horse\"}"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.token").value("token-123"));
	}

	@Test
	void loginReturnsUnauthorizedOnWrongPassword() throws Exception {
		when(authService.login("jane@example.com", "wrong-password"))
				.thenThrow(new InvalidCredentialsException("Invalid email or password"));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"email\":\"jane@example.com\",\"password\":\"wrong-password\"}"))
				.andExpect(MockMvcResultMatchers.status().isUnauthorized());
	}

	@Test
	void logoutReturnsNoContentAndRevokesTheTokenWhenTheTokenIsValid() throws Exception {
		JwtClaims claims = new JwtClaims("user-1", "jti-1", Instant.now().plusSeconds(60));
		when(jwtService.validateAndGetClaims("valid-token")).thenReturn(Optional.of(claims));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/logout")
						.header("Authorization", "Bearer valid-token"))
				.andExpect(MockMvcResultMatchers.status().isNoContent());

		verify(authService).logout(eq(claims));
	}

	@Test
	void logoutFailsLoudlyIfItSomehowReceivesATokenThatDoesNotValidate() {
		// Unreachable in real traffic - /api/auth/logout requires authentication, so
		// SecurityConfiguration would already have rejected this with a 401 before the
		// controller ever ran. Called directly rather than through MockMvc: an
		// uncaught exception here doesn't turn into an HTTP response within MockMvc's
		// simulated dispatch (that translation only happens in a real servlet
		// container), it propagates as a wrapped ServletException instead - so calling
		// the controller method directly is the more direct way to prove this guard
		// clause actually fires.
		when(jwtService.validateAndGetClaims("not-a-real-token")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authController.logout("Bearer not-a-real-token"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("Authenticated request reached the logout endpoint without valid claims");
	}

}
