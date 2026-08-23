package com.resumematcher.backend.ratelimit;

public record RateLimitDecision(boolean allowed, long retryAfterSeconds) {
}
