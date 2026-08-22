package com.resumematcher.backend.controller;

import static org.mockito.Mockito.when;

import java.util.Set;

import com.resumematcher.backend.matching.MatchResult;
import com.resumematcher.backend.matching.ResumeJobMatchService;
import com.resumematcher.backend.matching.ResumeNotFoundException;
import com.resumematcher.backend.matching.ResumeNotReadyException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(ResumeMatchController.class)
class ResumeMatchControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ResumeJobMatchService resumeJobMatchService;

	private static final String VALID_JOB_DESCRIPTION =
			"We are looking for a backend engineer experienced with Java, Spring Boot, Docker, "
					+ "and MySQL to join our growing platform team and help build scalable APIs.";

	@Test
	void returnsMatchResultWhenSuccessful() throws Exception {
		when(resumeJobMatchService.matchResumeToJobDescription("resume-1", VALID_JOB_DESCRIPTION))
				.thenReturn(new MatchResult(75, Set.of("java", "docker"), Set.of("aws")));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/resumes/resume-1/match")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"jobDescription\":\"" + VALID_JOB_DESCRIPTION + "\"}"))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.matchScorePercentage").value(75))
				.andExpect(MockMvcResultMatchers.jsonPath("$.matchedSkills[0]").value("java"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.missingSkills[0]").value("aws"));
	}

	@Test
	void returnsBadRequestWhenJobDescriptionTooShort() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/api/resumes/resume-1/match")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"jobDescription\":\"too short\"}"))
				.andExpect(MockMvcResultMatchers.status().isBadRequest())
				.andExpect(MockMvcResultMatchers.jsonPath("$.error").exists());
	}

	@Test
	void returnsNotFoundWhenResumeMissing() throws Exception {
		when(resumeJobMatchService.matchResumeToJobDescription("missing-id", VALID_JOB_DESCRIPTION))
				.thenThrow(new ResumeNotFoundException("No resume found with id: missing-id"));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/resumes/missing-id/match")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"jobDescription\":\"" + VALID_JOB_DESCRIPTION + "\"}"))
				.andExpect(MockMvcResultMatchers.status().isNotFound());
	}

	@Test
	void returnsBadRequestWhenResumeNotReady() throws Exception {
		when(resumeJobMatchService.matchResumeToJobDescription("resume-2", VALID_JOB_DESCRIPTION))
				.thenThrow(new ResumeNotReadyException("Resume has not finished processing yet"));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/resumes/resume-2/match")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"jobDescription\":\"" + VALID_JOB_DESCRIPTION + "\"}"))
				.andExpect(MockMvcResultMatchers.status().isBadRequest());
	}

}
