package com.resumematcher.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

	private static final String SECRET = "test-only-jwt-signing-secret-must-be-at-least-32-bytes";

	private final JwtService jwtService = new JwtService(SECRET, 24);

	@Test
	void generatesATokenThatValidatesBackToTheSameUserId() {
		String token = jwtService.generateToken("user-1");

		Optional<String> userId = jwtService.validateAndGetUserId(token);

		assertThat(userId).contains("user-1");
	}

	@Test
	void rejectsAMalformedToken() {
		Optional<String> userId = jwtService.validateAndGetUserId("not-a-real-token");

		assertThat(userId).isEmpty();
	}

	@Test
	void rejectsATokenSignedWithADifferentSecret() {
		JwtService otherService = new JwtService("a-completely-different-signing-secret-32-bytes-plus", 24);
		String token = otherService.generateToken("user-1");

		Optional<String> userId = jwtService.validateAndGetUserId(token);

		assertThat(userId).isEmpty();
	}

	@Test
	void rejectsAnExpiredToken() {
		SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
		Date past = new Date(System.currentTimeMillis() - 60_000);
		String expiredToken = Jwts.builder()
				.subject("user-1")
				.issuedAt(new Date(past.getTime() - 60_000))
				.expiration(past)
				.signWith(key)
				.compact();

		Optional<String> userId = jwtService.validateAndGetUserId(expiredToken);

		assertThat(userId).isEmpty();
	}

}
