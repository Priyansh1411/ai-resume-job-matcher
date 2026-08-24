package com.resumematcher.backend.security;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Tracks logged-out tokens by {@code jti} so {@link JwtAuthenticationFilter} can
 * reject them before their natural expiry, even though they'd otherwise still
 * pass signature/expiry validation. In-memory and single-node, deliberately
 * mirroring {@link com.resumematcher.backend.ratelimit.RateLimiterService}'s
 * existing shape: no distributed store, no DB table, no scheduled sweep -
 * entries are dropped lazily on read once their token would have expired
 * anyway. A restart clears all revocations, same tradeoff already accepted by
 * the rate limiter for single-node deployments.
 */
@Service
public class TokenRevocationService {

	private final ConcurrentHashMap<String, Instant> revokedTokenExpiries = new ConcurrentHashMap<>();

	public void revoke(String tokenId, Instant expiresAt) {
		revokedTokenExpiries.put(tokenId, expiresAt);
	}

	public boolean isRevoked(String tokenId) {
		Instant expiresAt = revokedTokenExpiries.get(tokenId);
		if (expiresAt == null) {
			return false;
		}
		if (Instant.now().isAfter(expiresAt)) {
			revokedTokenExpiries.remove(tokenId);
			return false;
		}
		return true;
	}

}
