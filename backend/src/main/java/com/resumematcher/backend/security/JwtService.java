package com.resumematcher.backend.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

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
				.issuedAt(issuedAt)
				.expiration(expiresAt)
				.signWith(signingKey)
				.compact();
	}

	public Optional<String> validateAndGetUserId(String token) {
		try {
			String userId = Jwts.parser()
					.verifyWith(signingKey)
					.build()
					.parseSignedClaims(token)
					.getPayload()
					.getSubject();
			return Optional.ofNullable(userId);
		} catch (JwtException | IllegalArgumentException e) {
			return Optional.empty();
		}
	}

}
