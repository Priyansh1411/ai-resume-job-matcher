package com.resumematcher.backend.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import com.resumematcher.backend.entity.ProcessingStatus;
import com.resumematcher.backend.entity.Resume;
import com.resumematcher.backend.extraction.ResumeTextExtractionService;
import com.resumematcher.backend.observability.ResumeProcessingMetrics;
import com.resumematcher.backend.profile.ResumeProfileExtractionService;
import com.resumematcher.backend.repository.ResumeRepository;
import com.resumematcher.backend.security.CurrentUserProvider;
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
	private final ResumeProfileExtractionService resumeProfileExtractionService;
	private final CurrentUserProvider currentUserProvider;
	private final ResumeProcessingMetrics resumeProcessingMetrics;

	public ResumeUploadService(ResumeRepository resumeRepository,
			ResumeTextExtractionService resumeTextExtractionService,
			ResumeProfileExtractionService resumeProfileExtractionService,
			CurrentUserProvider currentUserProvider,
			ResumeProcessingMetrics resumeProcessingMetrics) {
		this.resumeRepository = resumeRepository;
		this.resumeTextExtractionService = resumeTextExtractionService;
		this.resumeProfileExtractionService = resumeProfileExtractionService;
		this.currentUserProvider = currentUserProvider;
		this.resumeProcessingMetrics = resumeProcessingMetrics;
	}

	public Resume upload(MultipartFile file) {
		Instant start = Instant.now();
		try {
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

			// /api/resumes/upload requires authentication, so a missing user id here
			// means the security configuration let an unauthenticated request through -
			// a bug worth failing loudly on, not a case to degrade gracefully for.
			String ownerId = currentUserProvider.getCurrentUserId()
					.orElseThrow(() -> new IllegalStateException("Authenticated user id was not available during upload"));

			Resume resume = new Resume();
			resume.setOriginalFilename(cleanedFilename);
			resume.setContentType(contentType);
			resume.setFileSizeBytes(file.getSize());
			resume.setOwnerId(ownerId);

			Resume savedResume = resumeRepository.save(resume);

			extractAndStoreText(savedResume, file, contentType);

			return savedResume;
		} finally {
			resumeProcessingMetrics.recordUploadDuration(Duration.between(start, Instant.now()));
		}
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

		if (resume.getProcessingStatus() == ProcessingStatus.COMPLETED) {
			resumeProfileExtractionService.extractAndStoreProfile(resume.getId(), resume.getExtractedText());
		}
	}

}
