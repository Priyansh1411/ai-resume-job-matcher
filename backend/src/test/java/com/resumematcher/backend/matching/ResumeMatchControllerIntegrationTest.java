package com.resumematcher.backend.matching;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumematcher.backend.testsupport.AbstractMySqlIntegrationTest;
import com.resumematcher.backend.testsupport.SyntheticDocuments;
import com.resumematcher.backend.testsupport.TestAuthSupport;
import org.junit.jupiter.api.BeforeEach;
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
	private String authHeader;

	@BeforeEach
	void authenticate() throws Exception {
		authHeader = TestAuthSupport.registerAndGetAuthorizationHeader(mockMvc);
	}

	@Test
	void matchesUploadedResumeAgainstJobDescription() throws Exception {
		byte[] content = SyntheticDocuments.createSamplePdf(
				"Jane Doe\njane.doe@example.com\nSkilled in Java, Docker and MySQL.");
		MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", content);

		String uploadResponseBody = mockMvc.perform(
						MockMvcRequestBuilders.multipart("/api/resumes/upload").file(file)
								.header("Authorization", authHeader))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andReturn().getResponse().getContentAsString();

		String resumeId = objectMapper.readTree(uploadResponseBody).get("id").asText();

		String jobDescription = "We are looking for a backend engineer experienced with Java, "
				+ "Kubernetes and AWS to help build and operate our cloud platform at scale.";

		String matchResponseBody = mockMvc.perform(MockMvcRequestBuilders.post("/api/resumes/" + resumeId + "/match")
						.header("Authorization", authHeader)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"jobDescription\":\"" + jobDescription + "\"}"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andReturn().getResponse().getContentAsString();

		JsonNode matchJson = objectMapper.readTree(matchResponseBody);

		// Existing fields: this JD has no Required/Preferred headers, so scoring
		// must be identical to Phase 1's flat-coverage behavior.
		assertThat(matchJson.get("matchScorePercentage").asInt()).isEqualTo(33);
		assertThat(toList(matchJson.get("matchedSkills"))).containsExactly("java");
		assertThat(toList(matchJson.get("missingSkills"))).containsExactlyInAnyOrder("kubernetes", "aws");

		// New fields: with no sections detected, everything falls into the
		// required bucket and the preferred bucket stays empty.
		assertThat(toList(matchJson.get("matchedRequiredSkills"))).containsExactly("java");
		assertThat(toList(matchJson.get("missingRequiredSkills"))).containsExactlyInAnyOrder("kubernetes", "aws");
		assertThat(toList(matchJson.get("matchedPreferredSkills"))).isEmpty();
		assertThat(toList(matchJson.get("missingPreferredSkills"))).isEmpty();
	}

	@Test
	void matchWithExplicitRequiredAndPreferredSectionsReturnsCorrectBreakdown() throws Exception {
		byte[] content = SyntheticDocuments.createSamplePdf(
				"Jane Doe\njane.doe@example.com\nSkilled in Java, Docker and MySQL.");
		MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", content);

		String uploadResponseBody = mockMvc.perform(
						MockMvcRequestBuilders.multipart("/api/resumes/upload").file(file)
								.header("Authorization", authHeader))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andReturn().getResponse().getContentAsString();

		String resumeId = objectMapper.readTree(uploadResponseBody).get("id").asText();

		String jobDescription = "Required:\\nJava, Kubernetes\\n\\nPreferred:\\nAWS, MySQL\\n\\n"
				+ "Join our team to help us build and scale a modern, reliable cloud "
				+ "platform used by millions of people around the world every day.";

		String matchResponseBody = mockMvc.perform(MockMvcRequestBuilders.post("/api/resumes/" + resumeId + "/match")
						.header("Authorization", authHeader)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"jobDescription\":\"" + jobDescription + "\"}"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andReturn().getResponse().getContentAsString();

		JsonNode matchJson = objectMapper.readTree(matchResponseBody);

		// resume skills = {java, docker, mysql}; required = {java, kubernetes};
		// preferred = {aws, mysql}. weighted = (1*2 + 1*1) / (2*2 + 2*1) = 3/6 = 50%.
		assertThat(matchJson.get("matchScorePercentage").asInt()).isEqualTo(50);
		assertThat(toList(matchJson.get("matchedRequiredSkills"))).containsExactly("java");
		assertThat(toList(matchJson.get("missingRequiredSkills"))).containsExactly("kubernetes");
		assertThat(toList(matchJson.get("matchedPreferredSkills"))).containsExactly("mysql");
		assertThat(toList(matchJson.get("missingPreferredSkills"))).containsExactly("aws");

		// Existing fields stay the union of the required and preferred breakdowns.
		assertThat(toList(matchJson.get("matchedSkills"))).containsExactlyInAnyOrder("java", "mysql");
		assertThat(toList(matchJson.get("missingSkills"))).containsExactlyInAnyOrder("kubernetes", "aws");
	}

	private List<String> toList(JsonNode arrayNode) {
		List<String> values = new ArrayList<>();
		arrayNode.forEach(node -> values.add(node.asText()));
		return values;
	}

	@Test
	void returnsNotFoundForUnknownResumeId() throws Exception {
		String jobDescription = "We are looking for a backend engineer experienced with Java, "
				+ "Kubernetes and AWS to help build and operate our cloud platform at scale.";

		mockMvc.perform(MockMvcRequestBuilders.post("/api/resumes/does-not-exist/match")
						.header("Authorization", authHeader)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"jobDescription\":\"" + jobDescription + "\"}"))
				.andExpect(MockMvcResultMatchers.status().isNotFound());
	}

	@Test
	void rejectsMatchingWhenTheAuthenticatedUserDoesNotOwnTheResume() throws Exception {
		byte[] content = SyntheticDocuments.createSamplePdf(
				"Jane Doe\njane.doe@example.com\nSkilled in Java, Docker and MySQL.");
		MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", content);

		String uploadResponseBody = mockMvc.perform(
						MockMvcRequestBuilders.multipart("/api/resumes/upload").file(file)
								.header("Authorization", authHeader))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andReturn().getResponse().getContentAsString();
		String resumeId = objectMapper.readTree(uploadResponseBody).get("id").asText();

		// A second, different authenticated user - not the one who uploaded above.
		String otherUserAuthHeader = TestAuthSupport.registerAndGetAuthorizationHeader(mockMvc);

		String jobDescription = "We are looking for a backend engineer experienced with Java, "
				+ "Kubernetes and AWS to help build and operate our cloud platform at scale.";

		mockMvc.perform(MockMvcRequestBuilders.post("/api/resumes/" + resumeId + "/match")
						.header("Authorization", otherUserAuthHeader)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"jobDescription\":\"" + jobDescription + "\"}"))
				.andExpect(MockMvcResultMatchers.status().isForbidden());
	}

}
