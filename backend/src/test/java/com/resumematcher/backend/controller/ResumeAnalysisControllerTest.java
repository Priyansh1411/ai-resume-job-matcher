package com.resumematcher.backend.controller;

import static org.mockito.Mockito.when;

import java.util.List;

import com.resumematcher.backend.analysis.AnalysisUnavailableException;
import com.resumematcher.backend.analysis.ResumeAnalysisService;
import com.resumematcher.backend.dto.ResumeAnalysisResponse;
import com.resumematcher.backend.matching.ResumeNotFoundException;
import com.resumematcher.backend.matching.ResumeNotReadyException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(ResumeAnalysisController.class)
class ResumeAnalysisControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ResumeAnalysisService resumeAnalysisService;

	private static final String VALID_JOB_DESCRIPTION =
			"We are looking for a backend engineer experienced with Java, Spring Boot, Docker, "
					+ "and MySQL to join our growing platform team and help build scalable APIs.";

	@Test
	void returnsAnalysisResultWhenSuccessful() throws Exception {
		when(resumeAnalysisService.analyze("resume-1", VALID_JOB_DESCRIPTION))
				.thenReturn(new ResumeAnalysisResponse(
						List.of("Strong Java background"),
						List.of("No AWS experience listed"),
						List.of("Highlight cloud experience if any")));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/resumes/resume-1/analysis")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"jobDescription\":\"" + VALID_JOB_DESCRIPTION + "\"}"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.strengths")
						.value(Matchers.contains("Strong Java background")))
				.andExpect(MockMvcResultMatchers.jsonPath("$.gaps")
						.value(Matchers.contains("No AWS experience listed")))
				.andExpect(MockMvcResultMatchers.jsonPath("$.suggestions")
						.value(Matchers.contains("Highlight cloud experience if any")));
	}

	@Test
	void returnsBadRequestWhenJobDescriptionTooShort() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/api/resumes/resume-1/analysis")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"jobDescription\":\"too short\"}"))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.error").exists());
	}

	@Test
	void returnsNotFoundWhenResumeMissing() throws Exception {
		when(resumeAnalysisService.analyze("missing-id", VALID_JOB_DESCRIPTION))
				.thenThrow(new ResumeNotFoundException("No resume found with id: missing-id"));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/resumes/missing-id/analysis")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"jobDescription\":\"" + VALID_JOB_DESCRIPTION + "\"}"))
				.andExpect(MockMvcResultMatchers.status().isNotFound());
	}

	@Test
	void returnsBadRequestWhenResumeNotReady() throws Exception {
		when(resumeAnalysisService.analyze("resume-2", VALID_JOB_DESCRIPTION))
				.thenThrow(new ResumeNotReadyException("Resume has not finished processing yet"));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/resumes/resume-2/analysis")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"jobDescription\":\"" + VALID_JOB_DESCRIPTION + "\"}"))
				.andExpect(MockMvcResultMatchers.status().isBadRequest());
	}

	@Test
	void returnsServiceUnavailableWhenAnalysisIsDisabledOrProviderFails() throws Exception {
		when(resumeAnalysisService.analyze("resume-3", VALID_JOB_DESCRIPTION))
				.thenThrow(new AnalysisUnavailableException("AI analysis is not enabled"));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/resumes/resume-3/analysis")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"jobDescription\":\"" + VALID_JOB_DESCRIPTION + "\"}"))
				.andExpect(MockMvcResultMatchers.status().isServiceUnavailable());
	}

}
