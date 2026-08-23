package com.resumematcher.backend.controller;

import static org.mockito.Mockito.when;

import com.resumematcher.backend.dto.AuthResponse;
import com.resumematcher.backend.security.AuthService;
import com.resumematcher.backend.security.DuplicateEmailException;
import com.resumematcher.backend.security.InvalidCredentialsException;
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

	@MockitoBean
	private AuthService authService;

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

}
