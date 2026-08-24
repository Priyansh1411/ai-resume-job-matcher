package com.resumematcher.backend.security;

/**
 * Thrown by {@link AuthService#login} when the per-account login budget
 * (see {@link com.resumematcher.backend.ratelimit.RateLimitTier#AUTH_LOGIN_PER_ACCOUNT})
 * is exhausted. Distinct from the existing filter-based rejections in the
 * {@code ratelimit} package (which write the HTTP response directly and never
 * throw) because this check runs inside a service, not a filter, and needs to
 * signal AuthController through a normal exception path instead.
 */
public class RateLimitExceededException extends RuntimeException {

	private final long retryAfterSeconds;

	public RateLimitExceededException(long retryAfterSeconds) {
		super("Too many requests. Please try again later.");
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public long getRetryAfterSeconds() {
		return retryAfterSeconds;
	}

}
