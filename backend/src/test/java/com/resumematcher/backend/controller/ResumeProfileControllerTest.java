package com.resumematcher.backend.controller;

import static org.mockito.Mockito.when;

import java.util.List;

import com.resumematcher.backend.dto.ResumeProfileResponse;
import com.resumematcher.backend.entity.ProfileStatus;
import com.resumematcher.backend.profile.ResumeProfileNotFoundException;
import com.resumematcher.backend.profile.ResumeProfileQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(ResumeProfileController.class)
class ResumeProfileControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ResumeProfileQueryService resumeProfileQueryService;

	@Test
	void returnsProfileWhenFound() throws Exception {
		when(resumeProfileQueryService.getProfile("resume-1")).thenReturn(new ResumeProfileResponse(
				"Jane Doe", "jane@example.com", "555-1234", List.of("java", "docker"), ProfileStatus.COMPLETED));

		mockMvc.perform(MockMvcRequestBuilders.get("/api/resumes/resume-1/profile").accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isOk())
				.andExpect(MockMvcResultMatchers.jsonPath("$.fullName").value("Jane Doe"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.email").value("jane@example.com"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.phone").value("555-1234"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.skills[0]").value("java"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.skills[1]").value("docker"))
				.andExpect(MockMvcResultMatchers.jsonPath("$.profileStatus").value("COMPLETED"));
	}

	@Test
	void returnsNotFoundWhenProfileMissing() throws Exception {
		when(resumeProfileQueryService.getProfile("missing-id"))
				.thenThrow(new ResumeProfileNotFoundException("No resume profile found for resume id: missing-id"));

		mockMvc.perform(MockMvcRequestBuilders.get("/api/resumes/missing-id/profile").accept(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.status().isNotFound())
				.andExpect(MockMvcResultMatchers.jsonPath("$.error").exists());
	}

}
