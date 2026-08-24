package com.resumematcher.backend.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumematcher.backend.testsupport.AbstractMySqlIntegrationTest;
import com.resumematcher.backend.testsupport.SyntheticDocuments;
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
 * Proves {@link JsonAuthenticationEntryPoint} and {@link JsonAccessDeniedHandler}
 * are actually wired into the real filter chain end-to-end - not just correct in
 * isolation (see the corresponding unit tests) - and, just as importantly, that
 * wiring them in didn't change the status code used for ownership denial: 401 is
 * reserved for "not authenticated at all", 403 stays reserved for "authenticated
 * but not allowed", exactly as before.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityConfigurationIntegrationTest extends AbstractMySqlIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	// Must clear ResumeMatchRequest's 100-character minimum: for the no-token and
	// malformed-token cases the security filter chain rejects the request before
	// validation ever runs, but the ownership-denial case below does reach the
	// controller, so a too-short body would fail with 400 before ever reaching the
	// 403 this test is actually about.
	private static final String VALID_JOB_DESCRIPTION =
			"We are looking for a backend engineer experienced with Java, Spring Boot, Docker, "
					+ "and MySQL to join our growing platform team and help build scalable APIs.";

	@Test
	void returnsUnauthorizedWithAConsistentJsonBodyWhenNoTokenIsPresent() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/api/resumes/whatever/match")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"jobDescription\":\"" + VALID_JOB_DESCRIPTION + "\"}"))
				.andExpect(MockMvcResultMatchers.status().isUnauthorized())
				.andExpect(MockMvcResultMatchers.jsonPath("$.error")
						.value("Authentication is required to access this resource"));
	}

	@Test
	void returnsUnauthorizedWithAConsistentJsonBodyWhenTheTokenIsMalformed() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/api/resumes/whatever/match")
						.header("Authorization", "Bearer not-a-real-jwt")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"jobDescription\":\"" + VALID_JOB_DESCRIPTION + "\"}"))
				.andExpect(MockMvcResultMatchers.status().isUnauthorized())
				.andExpect(MockMvcResultMatchers.jsonPath("$.error")
						.value("Authentication is required to access this resource"));
	}

	@Test
	void stillReturnsForbiddenRatherThanUnauthorizedForOwnershipDenial() throws Exception {
		String ownerAuthHeader = TestAuthSupport.registerAndGetAuthorizationHeader(mockMvc);

		byte[] content = SyntheticDocuments.createSamplePdf(
				"Jane Doe\njane.doe@example.com\nSkilled in Java, Docker and MySQL.");
		MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", content);

		String uploadResponseBody = mockMvc.perform(
						MockMvcRequestBuilders.multipart("/api/resumes/upload").file(file)
								.header("Authorization", ownerAuthHeader))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andReturn().getResponse().getContentAsString();
		JsonNode uploadJson = objectMapper.readTree(uploadResponseBody);
		String resumeId = uploadJson.get("id").asText();

		// A second, different authenticated user - not the one who uploaded above.
		// Their token is perfectly valid, so this must stay 403 (authenticated but
		// not permitted), not fall through to the 401 path meant for missing/invalid
		// tokens.
		String otherUserAuthHeader = TestAuthSupport.registerAndGetAuthorizationHeader(mockMvc);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/resumes/" + resumeId + "/match")
						.header("Authorization", otherUserAuthHeader)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"jobDescription\":\"" + VALID_JOB_DESCRIPTION + "\"}"))
				.andExpect(MockMvcResultMatchers.status().isForbidden())
				.andExpect(MockMvcResultMatchers.jsonPath("$.error").value("Not authorized to access this resume"));
	}

}
