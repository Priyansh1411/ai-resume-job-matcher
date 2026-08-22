package com.resumematcher.backend.controller;

import java.util.Map;

import com.resumematcher.backend.dto.ResumeProfileResponse;
import com.resumematcher.backend.profile.ResumeProfileNotFoundException;
import com.resumematcher.backend.profile.ResumeProfileQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumeProfileController {

	private final ResumeProfileQueryService resumeProfileQueryService;

	public ResumeProfileController(ResumeProfileQueryService resumeProfileQueryService) {
		this.resumeProfileQueryService = resumeProfileQueryService;
	}

	@GetMapping("/api/resumes/{resumeId}/profile")
	public ResponseEntity<ResumeProfileResponse> getProfile(@PathVariable String resumeId) {
		return ResponseEntity.ok(resumeProfileQueryService.getProfile(resumeId));
	}

	@ExceptionHandler(ResumeProfileNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(ResumeProfileNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
	}

}
