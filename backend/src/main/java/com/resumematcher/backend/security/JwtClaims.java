package com.resumematcher.backend.security;

import java.time.Instant;

/**
 * Everything callers need out of a validated token in one parse. {@code tokenId}
 * and {@code expiresAt} exist solely to support revocation: {@link JwtAuthenticationFilter}
 * uses {@code tokenId} to check {@link TokenRevocationService#isRevoked}, and
 * {@link AuthService#logout} uses both to record how long a revocation entry
 * needs to live.
 */
public record JwtClaims(String userId, String tokenId, Instant expiresAt) {
}
