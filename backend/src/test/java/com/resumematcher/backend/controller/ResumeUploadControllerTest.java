package com.resumematcher.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumematcher.backend.entity.ProcessingStatus;
import com.resumematcher.backend.entity.Resume;
import com.resumematcher.backend.repository.ResumeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ResumeUploadControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ResumeRepository resumeRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void uploadsPdfResumeSuccessfully() throws Exception {
		byte[] content = "pdf content".getBytes();
		MockMultipartFile file = new MockMultipartFile("file", "resume.pdf", "application/pdf", content);

		String responseBody = mockMvc.perform(MockMvcRequestBuilders.multipart("/api/resumes/upload").file(file))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andReturn().getResponse().getContentAsString();

		JsonNode json = objectMapper.readTree(responseBody);

		assertThat(json.get("id").asText()).isNotBlank();
		assertThat(json.get("originalFilename").asText()).isEqualTo("resume.pdf");
		assertThat(json.get("contentType").asText()).isEqualTo("application/pdf");
		assertThat(json.get("fileSizeBytes").asLong()).isEqualTo(content.length);
		assertThat(json.get("processingStatus").asText()).isEqualTo("UPLOADED");

		Optional<Resume> saved = resumeRepository.findById(json.get("id").asText());
		assertThat(saved).isPresent();
		assertThat(saved.get().getOriginalFilename()).isEqualTo("resume.pdf");
		assertThat(saved.get().getContentType()).isEqualTo("application/pdf");
		assertThat(saved.get().getFileSizeBytes()).isEqualTo(content.length);
		assertThat(saved.get().getProcessingStatus()).isEqualTo(ProcessingStatus.UPLOADED);
		assertThat(saved.get().getExtractedText()).isNull();
	}

	@Test
	void uploadsDocxResumeSuccessfully() throws Exception {
		byte[] content = "docx content".getBytes();
		MockMultipartFile file = new MockMultipartFile(
				"file", "resume.docx",
				"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
				content);

		String responseBody = mockMvc.perform(MockMvcRequestBuilders.multipart("/api/resumes/upload").file(file))
				.andExpect(MockMvcResultMatchers.status().isCreated())
				.andReturn().getResponse().getContentAsString();

		JsonNode json = objectMapper.readTree(responseBody);

		assertThat(json.get("originalFilename").asText()).isEqualTo("resume.docx");
		assertThat(json.get("contentType").asText())
				.isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		assertThat(json.get("fileSizeBytes").asLong()).isEqualTo(content.length);
		assertThat(json.get("processingStatus").asText()).isEqualTo("UPLOADED");

		Optional<Resume> saved = resumeRepository.findById(json.get("id").asText());
		assertThat(saved).isPresent();
		assertThat(saved.get().getExtractedText()).isNull();
	}

	@Test
	void rejectsEmptyFile() throws Exception {
		long countBefore = resumeRepository.count();

		MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/resumes/upload").file(file))
				.andExpect(MockMvcResultMatchers.status().isBadRequest());

		assertThat(resumeRepository.count()).isEqualTo(countBefore);
	}

	@Test
	void rejectsUnsupportedContentType() throws Exception {
		long countBefore = resumeRepository.count();

		MockMultipartFile file = new MockMultipartFile("file", "resume.txt", "text/plain", "plain text".getBytes());

		mockMvc.perform(MockMvcRequestBuilders.multipart("/api/resumes/upload").file(file))
				.andExpect(MockMvcResultMatchers.status().isBadRequest());

		assertThat(resumeRepository.count()).isEqualTo(countBefore);
	}

	@Test
	void rejectsOrSanitizesUnsafeFilename() throws Exception {
		long countBefore = resumeRepository.count();

		MockMultipartFile file = new MockMultipartFile(
				"file", "../../resume.pdf", "application/pdf", "pdf content".getBytes());

		MvcResult result = mockMvc.perform(MockMvcRequestBuilders.multipart("/api/resumes/upload").file(file))
				.andReturn();

		int status = result.getResponse().getStatus();
		assertThat(status).isIn(201, 400);

		if (status == 201) {
			JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
			assertThat(json.get("originalFilename").asText()).doesNotContain("..");

			Optional<Resume> saved = resumeRepository.findById(json.get("id").asText());
			assertThat(saved).isPresent();
			assertThat(saved.get().getOriginalFilename()).doesNotContain("..");
		} else {
			assertThat(resumeRepository.count()).isEqualTo(countBefore);
		}
	}

}
