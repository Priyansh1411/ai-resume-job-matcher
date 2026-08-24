package com.resumematcher.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
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

		Optional<JwtClaims> claims = jwtService.validateAndGetClaims(token);

		assertThat(claims).isPresent();
		assertThat(claims.get().userId()).isEqualTo("user-1");
	}

	@Test
	void generatesADifferentTokenIdForEveryToken() {
		String first = jwtService.generateToken("user-1");
		String second = jwtService.generateToken("user-1");

		String firstTokenId = jwtService.validateAndGetClaims(first).get().tokenId();
		String secondTokenId = jwtService.validateAndGetClaims(second).get().tokenId();

		assertThat(firstTokenId).isNotBlank();
		assertThat(secondTokenId).isNotBlank();
		assertThat(firstTokenId).isNotEqualTo(secondTokenId);
	}

	@Test
	void claimsExpirationMatchesTheTokensActualExpiration() {
		SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
		Date issuedAt = new Date();
		Date expiration = new Date(issuedAt.getTime() + 60_000);
		String token = Jwts.builder()
				.subject("user-1")
				.id("fixed-token-id")
				.issuedAt(issuedAt)
				.expiration(expiration)
				.signWith(key)
				.compact();

		JwtClaims claims = jwtService.validateAndGetClaims(token).orElseThrow();

		assertThat(claims.tokenId()).isEqualTo("fixed-token-id");
		// The JWT "exp" claim is a NumericDate (RFC 7519 §2) - whole seconds, no
		// milliseconds - so it round-trips through encode/decode truncated to the
		// second even though the Date built above still carries millisecond precision.
		assertThat(claims.expiresAt()).isEqualTo(expiration.toInstant().truncatedTo(ChronoUnit.SECONDS));
	}

	@Test
	void rejectsAMalformedToken() {
		Optional<JwtClaims> claims = jwtService.validateAndGetClaims("not-a-real-token");

		assertThat(claims).isEmpty();
	}

	@Test
	void rejectsATokenSignedWithADifferentSecret() {
		JwtService otherService = new JwtService("a-completely-different-signing-secret-32-bytes-plus", 24);
		String token = otherService.generateToken("user-1");

		Optional<JwtClaims> claims = jwtService.validateAndGetClaims(token);

		assertThat(claims).isEmpty();
	}

	@Test
	void rejectsAnExpiredToken() {
		SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
		Date past = new Date(System.currentTimeMillis() - 60_000);
		String expiredToken = Jwts.builder()
				.subject("user-1")
				.id("expired-token-id")
				.issuedAt(new Date(past.getTime() - 60_000))
				.expiration(past)
				.signWith(key)
				.compact();

		Optional<JwtClaims> claims = jwtService.validateAndGetClaims(expiredToken);

		assertThat(claims).isEmpty();
	}

	@Test
	void rejectsATokenWithNoTokenId() {
		SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
		Date issuedAt = new Date();
		String tokenWithoutId = Jwts.builder()
				.subject("user-1")
				.issuedAt(issuedAt)
				.expiration(new Date(issuedAt.getTime() + 60_000))
				.signWith(key)
				.compact();

		Optional<JwtClaims> claims = jwtService.validateAndGetClaims(tokenWithoutId);

		assertThat(claims).isEmpty();
	}

}
