package com.resumematcher.backend.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Catches everything no controller's own {@code @ExceptionHandler} anticipates
 * - malformed input, routing failures, multipart problems, and any genuinely
 * unexpected exception - and normalizes it to this app's one JSON error shape,
 * instead of Spring Boot's default {@code {timestamp,status,error,path}} page
 * (or, for {@link MaxUploadSizeExceededException} specifically, an empty body -
 * both verified against the running app during Phase 11 planning).
 *
 * <p>{@code @RestControllerAdvice} handlers only run when no more specific
 * handler exists - every controller-local {@code @ExceptionHandler}
 * (DuplicateEmailException, ResumeAccessDeniedException, etc.) still wins for
 * the cases it already covers; this only fills the gaps around them, and
 * Spring Security's own filter-level handling (JsonAuthenticationEntryPoint,
 * JsonAccessDeniedHandler) is untouched - those run in the filter chain,
 * before DispatcherServlet's MVC exception resolution even starts.
 *
 * <p>Messages here are deliberately generic, fixed strings - never
 * {@code ex.getMessage()} - since these are exactly the exception types whose
 * own messages can't be trusted not to embed something internal (a Jackson
 * parse failure can quote the raw malformed input; a missing-parameter
 * exception names the exact parameter). The one exception:
 * {@link MaxUploadSizeExceededException} reuses the exact wording
 * ResumeUploadService's own size check already uses, so the message is
 * identical no matter which of the two checks caught it.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<Map<String, String>> handleMalformedRequestBody(HttpMessageNotReadableException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Malformed request body"));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<Map<String, String>> handleNoResourceFound(NoResourceFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Not found"));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<Map<String, String>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Map.of("error", "Method not allowed"));
	}

	// Not in the original verified list, but required for correctness: without
	// this, the broad Exception.class handler below would catch this type too
	// (Spring falls back to it whenever nothing more specific matches) and
	// downgrade a request Spring already handles correctly - e.g. POSTing to
	// /api/resumes/upload with no multipart Content-Type at all - from its
	// correct 415 to a misleading 500. Verified live: without this handler, that
	// exact request returns 500; with it, 415.
	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<Map<String, String>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
		return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(Map.of("error", "Unsupported content type"));
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	public ResponseEntity<Map<String, String>> handleMissingRequestPart(MissingServletRequestPartException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Required request part is missing"));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<Map<String, String>> handleMissingRequestParameter(
			MissingServletRequestParameterException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Required request parameter is missing"));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<Map<String, String>> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
				.body(Map.of("error", "Uploaded file exceeds the maximum allowed size"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, String>> handleUnexpectedException(Exception ex) {
		// Logged server-side only, at ERROR so it's never silently lost - the
		// client only ever sees the fixed, generic message below.
		log.error("Unhandled exception reached GlobalExceptionHandler", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Map.of("error", "An unexpected error occurred. Please try again."));
	}

}
