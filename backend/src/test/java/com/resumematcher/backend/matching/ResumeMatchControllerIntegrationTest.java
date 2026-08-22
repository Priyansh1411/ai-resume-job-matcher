package com.resumematcher.backend.matching;

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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ResumeMatchControllerIntegrationTest extends AbstractMySqlIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void matchesUploadedResumeAgainstJobDescription() throws Exception {
		byte[] content = SyntheticDocuments.createSamplePdf(
				"Jane Doe\njane.doe@example.com\nSkilled in Java, Docker and MySQL.");
		MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", content);

		String uploadResponseBody = mockMvc.perform(
						MockMvcRequestBuilders.multipart("/api/resumes/upload").file(file))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andReturn().getResponse().getContentAsString();

		String resumeId = objectMapper.readTree(uploadResponseBody).get("id").asText();

		String jobDescription = "We are looking for a backend engineer experienced with Java, "
				+ "Kubernetes and AWS to help build and operate our cloud platform at scale.";

		String matchResponseBody = mockMvc.perform(MockMvcRequestBuilders.post("/api/resumes/" + resumeId + "/match")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"jobDescription\":\"" + jobDescription + "\"}"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andReturn().getResponse().getContentAsString();

		JsonNode matchJson = objectMapper.readTree(matchResponseBody);

		assertThat(matchJson.get("matchScorePercentage").asInt()).isEqualTo(33);

		List<String> matchedSkills = new ArrayList<>();
		matchJson.get("matchedSkills").forEach(node -> matchedSkills.add(node.asText()));
		assertThat(matchedSkills).containsExactly("java");

		List<String> missingSkills = new ArrayList<>();
		matchJson.get("missingSkills").forEach(node -> missingSkills.add(node.asText()));
		assertThat(missingSkills).containsExactlyInAnyOrder("kubernetes", "aws");
	}

	@Test
	void returnsNotFoundForUnknownResumeId() throws Exception {
		String jobDescription = "We are looking for a backend engineer experienced with Java, "
				+ "Kubernetes and AWS to help build and operate our cloud platform at scale.";

		mockMvc.perform(MockMvcRequestBuilders.post("/api/resumes/does-not-exist/match")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"jobDescription\":\"" + jobDescription + "\"}"))
				.andExpect(MockMvcResultMatchers.status().isNotFound());
	}

}
