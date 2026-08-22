package com.resumematcher.backend.controller;

import java.util.Map;

import com.resumematcher.backend.dto.ResumeMatchRequest;
import com.resumematcher.backend.dto.ResumeMatchResponse;
import com.resumematcher.backend.matching.MatchResult;
import com.resumematcher.backend.matching.ResumeJobMatchService;
import com.resumematcher.backend.matching.ResumeNotFoundException;
import com.resumematcher.backend.matching.ResumeNotReadyException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ResumeMatchController {

	private final ResumeJobMatchService resumeJobMatchService;

	public ResumeMatchController(ResumeJobMatchService resumeJobMatchService) {
		this.resumeJobMatchService = resumeJobMatchService;
	}

	@PostMapping("/api/resumes/{resumeId}/match")
	public ResponseEntity<ResumeMatchResponse> matchResume(@PathVariable String resumeId,
			@Valid @RequestBody ResumeMatchRequest request) {
		MatchResult result = resumeJobMatchService.matchResumeToJobDescription(resumeId, request.jobDescription());

		ResumeMatchResponse response = new ResumeMatchResponse(
				result.matchScorePercentage(),
				result.matchedSkills(),
				result.missingSkills()
		);

		return ResponseEntity.ok(response);
	}

	@ExceptionHandler(ResumeNotFoundException.class)
	public ResponseEntity<Map<String, String>> handleNotFound(ResumeNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(ResumeNotReadyException.class)
	public ResponseEntity<Map<String, String>> handleNotReady(ResumeNotReadyException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, String>> handleValidationError(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.findFirst()
				.map(error -> error.getDefaultMessage())
				.orElse("Invalid request");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
	}

}
