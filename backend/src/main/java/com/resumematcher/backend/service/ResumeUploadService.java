package com.resumematcher.backend.service;

import java.util.Set;

import com.resumematcher.backend.entity.ProcessingStatus;
import com.resumematcher.backend.entity.Resume;
import com.resumematcher.backend.extraction.ResumeTextExtractionService;
import com.resumematcher.backend.repository.ResumeRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeUploadService {

	private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
			"application/pdf",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document"
	);

	private final ResumeRepository resumeRepository;
	private final ResumeTextExtractionService resumeTextExtractionService;

	public ResumeUploadService(ResumeRepository resumeRepository,
			ResumeTextExtractionService resumeTextExtractionService) {
		this.resumeRepository = resumeRepository;
		this.resumeTextExtractionService = resumeTextExtractionService;
	}

	public Resume upload(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new InvalidResumeUploadException("Uploaded file must not be empty");
		}

		String originalFilename = file.getOriginalFilename();
		if (!StringUtils.hasText(originalFilename)) {
			throw new InvalidResumeUploadException("Uploaded file must have a filename");
		}

		String cleanedFilename = StringUtils.cleanPath(originalFilename);
		if (cleanedFilename.contains("..")) {
			throw new InvalidResumeUploadException("Uploaded filename is not valid");
		}

		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
			throw new InvalidResumeUploadException("Unsupported file type");
		}

		if (file.getSize() > MAX_FILE_SIZE_BYTES) {
			throw new InvalidResumeUploadException("Uploaded file exceeds the maximum allowed size");
		}

		Resume resume = new Resume();
		resume.setOriginalFilename(cleanedFilename);
		resume.setContentType(contentType);
		resume.setFileSizeBytes(file.getSize());

		Resume savedResume = resumeRepository.save(resume);

		extractAndStoreText(savedResume, file, contentType);

		return savedResume;
	}

	private void extractAndStoreText(Resume resume, MultipartFile file, String contentType) {
		resume.setProcessingStatus(ProcessingStatus.PROCESSING);
		resumeRepository.save(resume);

		try {
			byte[] fileBytes = file.getBytes();
			String extractedText = resumeTextExtractionService.extractText(fileBytes, contentType);
			resume.setExtractedText(extractedText);
			resume.setProcessingStatus(ProcessingStatus.COMPLETED);
		} catch (Exception e) {
			resume.setExtractedText(null);
			resume.setProcessingStatus(ProcessingStatus.FAILED);
		}

		resumeRepository.save(resume);
	}

}
