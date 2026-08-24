package com.resumematcher.backend.controller;

import java.util.UUID;

import com.resumematcher.backend.testsupport.AbstractMySqlIntegrationTest;
import com.resumematcher.backend.testsupport.TestAuthSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proves {@link GlobalExceptionHandler} end-to-end through the real filter
 * chain and DispatcherServlet - every case here was first reproduced live
 * against a running instance during Phase 11 planning/implementation, where
 * each returned Spring Boot's raw {@code {timestamp,status,error,path}}
 * default page. The oversized-upload case (413, empty body) is covered
 * separately in {@link GlobalExceptionHandlerRealHttpIntegrationTest} instead
 * of here - see that class's javadoc for why MockMvc can't reproduce it.
 *
 * <p>Also includes a regression test proving a pre-existing, controller-local
 * {@code @ExceptionHandler} (DuplicateEmailException) still wins over the new
 * global advice, and a case ({@code HttpMediaTypeNotSupportedException}) found
 * and fixed during implementation: the broad catch-all below would otherwise
 * have swallowed it and downgraded a correct 415 into a misleading 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GlobalExceptionHandlerIntegrationTest extends AbstractMySqlIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void malformedJsonBodyReturnsBadRequestWithConsistentBody() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{not valid json"))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Malformed request body"));
	}

	@Test
	void unknownRouteReturnsNotFoundWithConsistentBody() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/does-not-exist"))
				.andExpect(MockMvcResultMatchers.status().isNotFound())
				.andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Not found"));
	}

	@Test
	void wrongHttpMethodReturnsMethodNotAllowedWithConsistentBody() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.delete("/api/auth/login"))
				.andExpect(MockMvcResultMatchers.status().isMethodNotAllowed())
				.andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Method not allowed"));
	}

	@Test
	void missingMultipartFilePartReturnsBadRequestWithConsistentBody() throws Exception {
		String authHeader = TestAuthSupport.registerAndGetAuthorizationHeader(mockMvc);
		MockMultipartFile wrongField = new MockMultipartFile("notfile", "irrelevant.txt", "text/plain",
				"irrelevant".getBytes());

		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/resumes/upload").file(wrongField)
						.header("Authorization", authHeader))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Required request part is missing"));
	}

	@Test
	void requestWithNoMultipartContentTypeReturnsUnsupportedMediaTypeNotInternalServerError() throws Exception {
		// Found during implementation: without an explicit handler for this
		// exception type, the broad Exception.class catch-all intercepts it too
		// (Spring falls back to the least-specific matching handler) and downgrades
		// this from its correct 415 to a misleading 500.
		String authHeader = TestAuthSupport.registerAndGetAuthorizationHeader(mockMvc);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/resumes/upload")
						.header("Authorization", authHeader))
				.andExpect(MockMvcResultMatchers.status().isUnsupportedMediaType())
				.andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Unsupported content type"));
	}

	@Test
	void existingControllerLocalHandlerStillTakesPrecedenceOverGlobalAdvice() throws Exception {
		String email = "duplicate-check-" + UUID.randomUUID() + "@example.com";
		String requestBody = "{\"email\":\"" + email + "\",\"password\":\"correct-horse-battery-staple\"}";

		mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(MockMvcResultMatchers.status().isCreated());

		// Registering the exact same email again must still hit AuthController's
		// own DuplicateEmailException handler (409, its own specific message) -
		// not the new global advice's generic handling of anything else.
		mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(MockMvcResultMatchers.status().isConflict())
				.andExpect(MockMvcResultMatchers.jsonPath("$.error").value("An account with this email already exists"));
	}

}
