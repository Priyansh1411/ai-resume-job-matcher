package com.resumematcher.backend.security;

import com.resumematcher.backend.testsupport.AbstractMySqlIntegrationTest;
import com.resumematcher.backend.testsupport.SyntheticDocuments;
import com.resumematcher.backend.testsupport.TestAuthSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proves logout actually revokes the token end-to-end through the real filter
 * chain, not just at the unit level (see JwtAuthenticationFilterTest and
 * TokenRevocationServiceTest for the isolated pieces): the exact same token
 * that worked a moment ago must stop working immediately after logout,
 * without waiting for its natural expiry.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LogoutIntegrationTest extends AbstractMySqlIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void theSameTokenIsRejectedAfterLogout() throws Exception {
		String authHeader = TestAuthSupport.registerAndGetAuthorizationHeader(mockMvc);

		byte[] content = SyntheticDocuments.createSamplePdf(
				"Jane Doe\njane.doe@example.com\nSkilled in Java, Docker and MySQL.");
		MockMultipartFile firstUpload = new MockMultipartFile("file", "resume.pdf", "application/pdf", content);

		// The token works before logout.
		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/resumes/upload").file(firstUpload)
						.header("Authorization", authHeader))
				.andExpect(MockMvcResultMatchers.status().isCreated());

		mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/logout")
						.header("Authorization", authHeader))
				.andExpect(MockMvcResultMatchers.status().isNoContent());

		// The exact same token, reused after logout, must now be rejected - not
		// after its natural 24h expiry, but immediately.
		MockMultipartFile secondUpload = new MockMultipartFile("file", "resume.pdf", "application/pdf", content);
		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/resumes/upload").file(secondUpload)
						.header("Authorization", authHeader))
				.andExpect(MockMvcResultMatchers.status().isUnauthorized())
				.andExpect(MockMvcResultMatchers.jsonPath("$.error")
						.value("Authentication is required to access this resource"));
	}

	@Test
	void logoutItselfRequiresAValidToken() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/logout"))
				.andExpect(MockMvcResultMatchers.status().isUnauthorized())
				.andExpect(MockMvcResultMatchers.jsonPath("$.error")
						.value("Authentication is required to access this resource"));
	}

	@Test
	void loggingOutOneTokenDoesNotAffectAnotherUsersToken() throws Exception {
		String authHeaderA = TestAuthSupport.registerAndGetAuthorizationHeader(mockMvc);
		String authHeaderB = TestAuthSupport.registerAndGetAuthorizationHeader(mockMvc);

		mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/logout")
						.header("Authorization", authHeaderA))
				.andExpect(MockMvcResultMatchers.status().isNoContent());

		byte[] content = SyntheticDocuments.createSamplePdf(
				"Jane Doe\njane.doe@example.com\nSkilled in Java, Docker and MySQL.");
		MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", content);

		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/resumes/upload").file(file)
						.header("Authorization", authHeaderB))
				.andExpect(MockMvcResultMatchers.status().isCreated());
	}

}
