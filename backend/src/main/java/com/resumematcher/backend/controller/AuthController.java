package com.resumematcher.backend.controller;

import java.util.Map;

import com.resumematcher.backend.dto.AuthResponse;
import com.resumematcher.backend.dto.LoginRequest;
import com.resumematcher.backend.dto.RegisterRequest;
import com.resumematcher.backend.security.AuthService;
import com.resumematcher.backend.security.DuplicateEmailException;
import com.resumematcher.backend.security.InvalidCredentialsException;
import com.resumematcher.backend.security.JwtClaims;
import com.resumematcher.backend.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

	private static final String BEARER_PREFIX = "Bearer ";

	private final AuthService authService;
	private final JwtService jwtService;

	public AuthController(AuthService authService, JwtService jwtService) {
		this.authService = authService;
		this.jwtService = jwtService;
	}

	@PostMapping("/api/auth/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		AuthResponse response = authService.register(request.email(), request.password());
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@PostMapping("/api/auth/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		AuthResponse response = authService.login(request.email(), request.password());
		return ResponseEntity.ok(response);
	}

	@PostMapping("/api/auth/logout")
	public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorizationHeader) {
		String token = authorizationHeader.substring(BEARER_PREFIX.length());

		// /api/auth/logout requires authentication, so reaching here without valid,
		// non-revoked claims means the security configuration let an unauthenticated
		// request through - a bug worth failing loudly on, not a case to degrade
		// gracefully for. Mirrors ResumeUploadService's identical reasoning for its
		// own "security guarantees this" invariant.
		JwtClaims claims = jwtService.validateAndGetClaims(token)
				.orElseThrow(() -> new IllegalStateException(
						"Authenticated request reached the logout endpoint without valid claims"));

		authService.logout(claims);
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(DuplicateEmailException.class)
	public ResponseEntity<Map<String, String>> handleDuplicateEmail(DuplicateEmailException ex) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ex.getMessage()));
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ex.getMessage()));
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
