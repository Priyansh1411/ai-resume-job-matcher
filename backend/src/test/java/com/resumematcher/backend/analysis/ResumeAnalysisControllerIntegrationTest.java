package com.resumematcher.backend.analysis;

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
@TestPropertySource(properties = "analysis.enabled=true")
class ResumeAnalysisControllerIntegrationTest extends AbstractMySqlIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OpenAiChatClient chatClient;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void returnsTheParsedAnalysisEndToEndWhenEnabled() throws Exception {
		when(chatClient.complete(anyString(), anyString())).thenReturn(
				"{\"strengths\":[\"Strong Java background\"],"
						+ "\"gaps\":[\"No AWS experience listed\"],"
						+ "\"suggestions\":[\"Highlight cloud experience if any\"]}");

		String resumeId = uploadSyntheticResume();
		String jobDescription = "We are looking for a backend engineer experienced with Java, "
				+ "Kubernetes and AWS to help build and operate our cloud platform at scale.";

		String responseBody = mockMvc.perform(MockMvcRequestBuilders.post("/api/resumes/" + resumeId + "/analysis")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"jobDescription\":\"" + jobDescription + "\"}"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andReturn().getResponse().getContentAsString();

		JsonNode json = objectMapper.readTree(responseBody);
		assertThat(json.get("strengths").toString()).contains("Strong Java background");
		assertThat(json.get("gaps").toString()).contains("No AWS experience listed");
		assertThat(json.get("suggestions").toString()).contains("Highlight cloud experience if any");
	}

	@Test
	void returnsServiceUnavailableEndToEndWhenTheProviderFails() throws Exception {
		when(chatClient.complete(anyString(), anyString()))
				.thenThrow(new AnalysisUnavailableException("provider unreachable"));

		String resumeId = uploadSyntheticResume();
		String jobDescription = "We are looking for a backend engineer experienced with Java, "
				+ "Kubernetes and AWS to help build and operate our cloud platform at scale.";

		mockMvc.perform(MockMvcRequestBuilders.post("/api/resumes/" + resumeId + "/analysis")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"jobDescription\":\"" + jobDescription + "\"}"))
				.andExpect(MockMvcResultMatchers.status().isServiceUnavailable());
	}

	private String uploadSyntheticResume() throws Exception {
		byte[] content = SyntheticDocuments.createSamplePdf(
				"Jane Doe\njane.doe@example.com\nSkilled in Java, Docker and MySQL.");
		MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", content);

		String uploadResponseBody = mockMvc.perform(
						MockMvcRequestBuilders.multipart("/api/resumes/upload").file(file))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andReturn().getResponse().getContentAsString();

		return objectMapper.readTree(uploadResponseBody).get("id").asText();
	}

}
