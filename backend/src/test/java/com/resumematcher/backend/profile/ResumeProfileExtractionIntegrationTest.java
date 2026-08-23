package com.resumematcher.backend.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumematcher.backend.entity.ProfileStatus;
import com.resumematcher.backend.entity.ResumeProfile;
import com.resumematcher.backend.entity.ResumeSkill;
import com.resumematcher.backend.repository.ResumeProfileRepository;
import com.resumematcher.backend.repository.ResumeSkillRepository;
import com.resumematcher.backend.testsupport.AbstractMySqlIntegrationTest;
import com.resumematcher.backend.testsupport.SyntheticDocuments;
import com.resumematcher.backend.testsupport.TestAuthSupport;
import org.junit.jupiter.api.BeforeEach;
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
class ResumeProfileExtractionIntegrationTest extends AbstractMySqlIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ResumeProfileRepository resumeProfileRepository;

	@Autowired
	private ResumeSkillRepository resumeSkillRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private String authHeader;

	@BeforeEach
	void authenticate() throws Exception {
		authHeader = TestAuthSupport.registerAndGetAuthorizationHeader(mockMvc);
	}

	@Test
	void uploadingResumeWithRecognizableTextCreatesCompletedProfileAndSkills() throws Exception {
		byte[] content = SyntheticDocuments.createSamplePdf(
				"Jane Doe jane.doe@example.com Skilled in Java, Docker and MySQL.");
		MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", content);

		String responseBody = mockMvc.perform(MockMvcRequestBuilders.multipart("/api/resumes/upload").file(file)
						.header("Authorization", authHeader))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andReturn().getResponse().getContentAsString();

		JsonNode json = objectMapper.readTree(responseBody);
		String resumeId = json.get("id").asText();

		Optional<ResumeProfile> profile = resumeProfileRepository.findByResumeId(resumeId);
		assertThat(profile).isPresent();
		assertThat(profile.get().getProfileStatus()).isEqualTo(ProfileStatus.COMPLETED);
		assertThat(profile.get().getEmail()).isEqualTo("jane.doe@example.com");

		List<ResumeSkill> skills = resumeSkillRepository.findByResumeId(resumeId);
		assertThat(skills).extracting(ResumeSkill::getSkillName).contains("java", "docker", "mysql");
	}

	@Test
	void uploadWithCorruptFileNeverCreatesProfileRow() throws Exception {
		byte[] corruptContent = "this is not a real pdf file".getBytes();
		MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", corruptContent);

		String responseBody = mockMvc.perform(MockMvcRequestBuilders.multipart("/api/resumes/upload").file(file)
						.header("Authorization", authHeader))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andReturn().getResponse().getContentAsString();

		JsonNode json = objectMapper.readTree(responseBody);
		String resumeId = json.get("id").asText();

		assertThat(json.get("processingStatus").asText()).isEqualTo("FAILED");
		assertThat(resumeProfileRepository.findByResumeId(resumeId)).isEmpty();
	}

}
