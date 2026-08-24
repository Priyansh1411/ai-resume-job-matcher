package com.resumematcher.backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

	private final SecretKey signingKey;
	private final Duration expiration;

	public JwtService(
			@Value("${jwt.secret}") String secret,
			@Value("${jwt.expiration-hours:24}") long expirationHours) {
		this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expiration = Duration.ofHours(expirationHours);
	}

	public String generateToken(String userId) {
		Date issuedAt = new Date();
		Date expiresAt = new Date(issuedAt.getTime() + expiration.toMillis());

		return Jwts.builder()
				.subject(userId)
				.id(UUID.randomUUID().toString())
				.issuedAt(issuedAt)
				.expiration(expiresAt)
				.signWith(signingKey)
				.compact();
	}

	public Optional<JwtClaims> validateAndGetClaims(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(signingKey)
					.build()
					.parseSignedClaims(token)
					.getPayload();

			String userId = claims.getSubject();
			String tokenId = claims.getId();
			if (userId == null || tokenId == null) {
				return Optional.empty();
			}

			return Optional.of(new JwtClaims(userId, tokenId, claims.getExpiration().toInstant()));
		} catch (JwtException | IllegalArgumentException e) {
			return Optional.empty();
		}
	}

}
