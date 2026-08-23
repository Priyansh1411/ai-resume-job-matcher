package com.resumematcher.backend.matching;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "matching.semantic.enabled=true")
class ResumeMatchControllerSemanticIntegrationTest extends AbstractMySqlIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EmbeddingClient embeddingClient;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void usesTheSemanticScoreEndToEndWhenSemanticMatchingIsEnabled() throws Exception {
		when(embeddingClient.embed(anyString())).thenReturn(new float[] { 1f, 0f });

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

		// The mocked client always returns identical vectors, so cosine similarity
		// is 1.0 => 100%. Plain keyword coverage for this input would be 33%, so a
		// 100% result proves the semantic path (not the keyword fallback) ran.
		assertThat(matchJson.get("matchScorePercentage").asInt()).isEqualTo(100);

		// The skill breakdown must still be the keyword-derived one.
		assertThat(matchJson.get("matchedSkills").toString()).contains("java");
		assertThat(matchJson.get("missingSkills").toString()).contains("kubernetes", "aws");
	}

	@Test
	void fallsBackToKeywordScoreEndToEndWhenTheEmbeddingProviderFails() throws Exception {
		when(embeddingClient.embed(anyString())).thenThrow(new EmbeddingException("provider unreachable"));

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

		// The embedding provider always fails, so this must equal the exact
		// keyword-only score for this input (proven elsewhere to be 33%).
		assertThat(matchJson.get("matchScorePercentage").asInt()).isEqualTo(33);
	}

}
