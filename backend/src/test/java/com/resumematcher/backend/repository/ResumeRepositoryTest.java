package com.resumematcher.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import com.resumematcher.backend.entity.ProcessingStatus;
import com.resumematcher.backend.entity.Resume;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ResumeRepositoryTest {

	@Autowired
	private ResumeRepository resumeRepository;

	@Test
	void savesAndRetrievesResumeMetadata() {
		Resume resume = new Resume();
		resume.setOriginalFilename("test-resume.pdf");
		resume.setContentType("application/pdf");
		resume.setFileSizeBytes(1024L);

		Resume saved = resumeRepository.save(resume);

		assertThat(saved.getId()).isNotNull();
		assertThat(saved.getProcessingStatus()).isEqualTo(ProcessingStatus.UPLOADED);

		Optional<Resume> found = resumeRepository.findById(saved.getId());

		assertThat(found).isPresent();
		assertThat(found.get().getOriginalFilename()).isEqualTo("test-resume.pdf");
		assertThat(found.get().getContentType()).isEqualTo("application/pdf");
		assertThat(found.get().getFileSizeBytes()).isEqualTo(1024L);
	}

}
