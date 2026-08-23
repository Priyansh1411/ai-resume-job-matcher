package com.resumematcher.backend.controller;

import java.util.Map;

import com.resumematcher.backend.analysis.AnalysisUnavailableException;
import com.resumematcher.backend.analysis.ResumeAnalysisService;
import com.resumematcher.backend.dto.AnalysisRequest;
import com.resumematcher.backend.dto.ResumeAnalysisResponse;
import com.resumematcher.backend.matching.ResumeNotFoundException;
import com.resumematcher.backend.matching.ResumeNotReadyException;
import com.resumematcher.backend.security.ResumeAccessDeniedException;
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
public class ResumeAnalysisController {

	private final ResumeAnalysisService resumeAnalysisService;

	public ResumeAnalysisController(ResumeAnalysisService resumeAnalysisService) {
		this.resumeAnalysisService = resumeAnalysisService;
	}

	@PostMapping("/api/resumes/{resumeId}/analysis")
	public ResponseEntity<ResumeAnalysisResponse> analyzeResume(@PathVariable String resumeId,
			@Valid @RequestBody AnalysisRequest request) {
		ResumeAnalysisResponse response = resumeAnalysisService.analyze(resumeId, request.jobDescription());
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

	@ExceptionHandler(AnalysisUnavailableException.class)
	public ResponseEntity<Map<String, String>> handleUnavailable(AnalysisUnavailableException ex) {
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(ResumeAccessDeniedException.class)
	public ResponseEntity<Map<String, String>> handleAccessDenied(ResumeAccessDeniedException ex) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ex.getMessage()));
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
