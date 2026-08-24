package com.resumematcher.backend.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class TokenRevocationServiceTest {

	private final TokenRevocationService service = new TokenRevocationService();

	@Test
	void aTokenThatWasNeverRevokedIsNotRevoked() {
		assertThat(service.isRevoked("some-token-id")).isFalse();
	}

	@Test
	void aRevokedTokenIsRevoked() {
		service.revoke("token-1", Instant.now().plusSeconds(60));

		assertThat(service.isRevoked("token-1")).isTrue();
	}

	@Test
	void revokingOneTokenDoesNotAffectAnother() {
		service.revoke("token-1", Instant.now().plusSeconds(60));

		assertThat(service.isRevoked("token-2")).isFalse();
	}

	@Test
	void aRevocationEntryPastItsExpiryNoLongerCountsAsRevoked() {
		service.revoke("token-1", Instant.now().minusSeconds(1));

		// The token itself would already fail JwtService's own expiry check by this
		// point, so there's no security gap here - this just proves the revocation
		// entry doesn't linger and get treated as "still revoked" indefinitely.
		assertThat(service.isRevoked("token-1")).isFalse();
	}

}
