package com.resumematcher.backend.testsupport;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Registers a throwaway user through the real /api/auth/register endpoint and
 * returns a ready-to-use "Bearer &lt;token&gt;" header value, so integration tests
 * exercising the now-authenticated /api/resumes/** endpoints don't each need to
 * hand-roll registration.
 */
public final class TestAuthSupport {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private TestAuthSupport() {
	}

	public static String registerAndGetAuthorizationHeader(MockMvc mockMvc) throws Exception {
		String email = "test-user-" + UUID.randomUUID() + "@example.com";
		String requestBody = "{\"email\":\"" + email + "\",\"password\":\"correct-horse-battery-staple\"}";

		String responseBody = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andReturn().getResponse().getContentAsString();

		JsonNode json = OBJECT_MAPPER.readTree(responseBody);
		return "Bearer " + json.get("token").asText();
	}

}
