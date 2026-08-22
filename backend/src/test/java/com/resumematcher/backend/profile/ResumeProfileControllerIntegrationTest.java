package com.resumematcher.backend.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumematcher.backend.testsupport.AbstractMySqlIntegrationTest;
import com.resumematcher.backend.testsupport.SyntheticDocuments;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ResumeProfileControllerIntegrationTest extends AbstractMySqlIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void retrievesProfileAfterSuccessfulUpload() throws Exception {
		byte[] content = SyntheticDocuments.createSamplePdf(
				"Jane Doe\njane.doe@example.com\nSkilled in Java, Docker and MySQL.");
		MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", content);

		String uploadResponseBody = mockMvc.perform(
						MockMvcRequestBuilders.multipart("/api/resumes/upload").file(file))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andReturn().getResponse().getContentAsString();

		String resumeId = objectMapper.readTree(uploadResponseBody).get("id").asText();

		String profileResponseBody = mockMvc.perform(
						MockMvcRequestBuilders.get("/api/resumes/" + resumeId + "/profile"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andReturn().getResponse().getContentAsString();

		JsonNode profileJson = objectMapper.readTree(profileResponseBody);

		assertThat(profileJson.get("fullName").asText()).isEqualTo("Jane Doe");
		assertThat(profileJson.get("email").asText()).isEqualTo("jane.doe@example.com");
		assertThat(profileJson.get("profileStatus").asText()).isEqualTo("COMPLETED");

		JsonNode skills = profileJson.get("skills");
		assertThat(skills.isArray()).isTrue();
		List<String> skillNames = new ArrayList<>();
		skills.forEach(node -> skillNames.add(node.asText()));
		assertThat(skillNames).contains("java", "docker", "mysql");

		assertThat(profileJson.has("extractedText")).isFalse();
	}

	@Test
	void returnsNotFoundForUnknownResumeId() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/resumes/does-not-exist/profile"))
				.andExpect(MockMvcResultMatchers.status().isNotFound());
	}

}
