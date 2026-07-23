package com.resumematcher.backend.controller;

import java.util.Map;

import com.resumematcher.backend.dto.ResumeUploadResponse;
import com.resumematcher.backend.entity.Resume;
import com.resumematcher.backend.service.InvalidResumeUploadException;
import com.resumematcher.backend.service.ResumeUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ResumeUploadController {

	private final ResumeUploadService resumeUploadService;

	public ResumeUploadController(ResumeUploadService resumeUploadService) {
		this.resumeUploadService = resumeUploadService;
	}

	@PostMapping(value = "/api/resumes/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ResumeUploadResponse> uploadResume(@RequestParam("file") MultipartFile file) {
		Resume resume = resumeUploadService.upload(file);

		ResumeUploadResponse response = new ResumeUploadResponse(
				resume.getId(),
				resume.getOriginalFilename(),
				resume.getContentType(),
				resume.getFileSizeBytes(),
				resume.getProcessingStatus()
		);

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@ExceptionHandler(InvalidResumeUploadException.class)
	public ResponseEntity<Map<String, String>> handleInvalidUpload(InvalidResumeUploadException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
	}

}
